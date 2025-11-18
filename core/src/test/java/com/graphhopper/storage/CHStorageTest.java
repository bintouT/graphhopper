package com.graphhopper.storage;

import com.graphhopper.routing.ch.PrepareEncoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.graphhopper.routing.ch.CHConfig;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.NodeAccess;

import java.util.function.Consumer;

import static org.mockito.Mockito.*;


import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CHStorageTest {

    @Test
    void setAndGetLevels() {
        RAMDirectory dir = new RAMDirectory();
        CHStorage store = new CHStorage(dir, "ch1", -1, false);
        store.create(30, 5);
        assertEquals(0, store.getLevel(store.toNodePointer(10)));
        store.setLevel(store.toNodePointer(10), 100);
        assertEquals(100, store.getLevel(store.toNodePointer(10)));
        store.setLevel(store.toNodePointer(29), 300);
        assertEquals(300, store.getLevel(store.toNodePointer(29)));
    }

    @Test
    void createAndLoad(@TempDir Path path) {
        {
            GHDirectory dir = new GHDirectory(path.toAbsolutePath().toString(), DAType.RAM_INT_STORE);
            CHStorage chStorage = new CHStorage(dir, "car", -1, false);
            // we have to call create, because we want to create a new storage not load an existing one
            chStorage.create(5, 3);
            assertEquals(0, chStorage.shortcutNodeBased(0, 1, PrepareEncoder.getScFwdDir(), 10, 3, 5));
            assertEquals(1, chStorage.shortcutNodeBased(1, 2, PrepareEncoder.getScFwdDir(), 11, 4, 6));
            assertEquals(2, chStorage.shortcutNodeBased(2, 3, PrepareEncoder.getScFwdDir(), 12, 5, 7));
            // exceeding the number of expected shortcuts is ok, the container will just grow
            assertEquals(3, chStorage.shortcutNodeBased(3, 4, PrepareEncoder.getScFwdDir(), 13, 6, 8));
            assertEquals(5, chStorage.getNodes());
            assertEquals(4, chStorage.getShortcuts());
            chStorage.flush();
            chStorage.close();
        }
        {
            GHDirectory dir = new GHDirectory(path.toAbsolutePath().toString(), DAType.RAM_INT_STORE);
            CHStorage chStorage = new CHStorage(dir, "car", -1, false);
            // this time we load from disk
            chStorage.loadExisting();
            assertEquals(4, chStorage.getShortcuts());
            assertEquals(5, chStorage.getNodes());
            long ptr = chStorage.toShortcutPointer(0);
            assertEquals(0, chStorage.getNodeA(ptr));
            assertEquals(1, chStorage.getNodeB(ptr));
            assertEquals(10, chStorage.getWeight(ptr));
            assertEquals(3, chStorage.getSkippedEdge1(ptr));
            assertEquals(5, chStorage.getSkippedEdge2(ptr));
        }
    }

    @Test
    public void testBigWeight() {
        CHStorage g = new CHStorage(new RAMDirectory(), "abc", 1024, false);
        g.shortcutNodeBased(0, 0, 0, 10, 0, 1);

        g.setWeight(0, Integer.MAX_VALUE / 1000d + 1000);
        assertEquals(Integer.MAX_VALUE / 1000d + 1000, g.getWeight(0));

        g.setWeight(0, ((long) Integer.MAX_VALUE << 1) / 1000d - 0.001);
        assertEquals(((long) Integer.MAX_VALUE << 1) / 1000d - 0.001, g.getWeight(0), 0.001);

        g.setWeight(0, ((long) Integer.MAX_VALUE << 1) / 1000d);
        assertTrue(Double.isInfinite(g.getWeight(0)));
        g.setWeight(0, ((long) Integer.MAX_VALUE << 1) / 1000d + 1);
        assertTrue(Double.isInfinite(g.getWeight(0)));
        g.setWeight(0, ((long) Integer.MAX_VALUE << 1) / 1000d + 100);
        assertTrue(Double.isInfinite(g.getWeight(0)));
    }

    @Test
    public void testLargeNodeA() {
        int nodeA = Integer.MAX_VALUE;
        RAMIntDataAccess access = new RAMIntDataAccess("", "", false, -1);
        access.create(1000);
        access.setInt(0, nodeA << 1 | 1 & PrepareEncoder.getScFwdDir());
        assertTrue(access.getInt(0) < 0);
        assertEquals(Integer.MAX_VALUE, access.getInt(0) >>> 1);
    }
    // Cas 1 : fromGraph doit refuser un graphe non figé
    @Test
    void testFromGraphNotFrozen() {
        BaseGraph baseGraph = mock(BaseGraph.class);
        CHConfig chConfig = mock(CHConfig.class);

        when(baseGraph.isFrozen()).thenReturn(false);

        // Vérifie qu'on lève une exception si le graphe n'est pas frozen
        assertThrows(IllegalStateException.class, () -> CHStorage.fromGraph(baseGraph, chConfig));
    }

    // Cas 2 : fromGraph crée un stockage edge-based cohérent
    @Test
    void testFromGraphEdgeBased(@TempDir Path path) {
        BaseGraph baseGraph = mock(BaseGraph.class);
        CHConfig chConfig = mock(CHConfig.class);
        NodeAccess nodeAccess = mock(NodeAccess.class);

        GHDirectory dir = new GHDirectory(path.toAbsolutePath().toString(), DAType.RAM_INT_STORE);

        when(baseGraph.isFrozen()).thenReturn(true);
        when(baseGraph.getDirectory()).thenReturn(dir);
        when(baseGraph.getSegmentSize()).thenReturn(-1);
        when(baseGraph.getEdges()).thenReturn(10);
        when(baseGraph.getNodes()).thenReturn(5);
        when(baseGraph.getNodeAccess()).thenReturn(nodeAccess);

        when(chConfig.getName()).thenReturn("car");
        when(chConfig.isEdgeBased()).thenReturn(true);

        CHStorage storage = CHStorage.fromGraph(baseGraph, chConfig);

        // Vérifie que la config edge-based et le nombre de nœuds sont bien repris
        assertTrue(storage.isEdgeBased());
        assertEquals(5, storage.getNodes());
        assertEquals(0, storage.getShortcuts());
    }

    // Cas 3 : un poids trop petit déclenche le consumer de poids faibles
    @Test
    void testLowWeightShortcutCallsConsumer() {
        RAMDirectory dir = new RAMDirectory();
        CHStorage storage = new CHStorage(dir, "ch-low", -1, false);
        storage.create(1, 1);

        @SuppressWarnings("unchecked")
        Consumer<CHStorage.LowWeightShortcut> consumer = mock(Consumer.class);
        storage.setLowShortcutWeightConsumer(consumer);

        double tinyWeight = 0.0001;

        int shortcutIndex = storage.shortcutNodeBased(
                0, 0, PrepareEncoder.getScFwdDir(), tinyWeight, 1, 2
        );

        // Vérifie que le premier raccourci appelle bien le consumer avec des infos cohérentes
        assertEquals(0, shortcutIndex);
        verify(consumer).accept(argThat(s ->
                s.nodeA == 0 &&
                s.nodeB == 0 &&
                s.shortcut == 0 &&
                s.weight == tinyWeight
        ));
    }
}
