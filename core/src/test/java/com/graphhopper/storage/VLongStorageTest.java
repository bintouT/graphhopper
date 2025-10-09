/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Karich
 */
public class VLongStorageTest {
    @Test
    public void testWrite() {
        VLongStorage store = new VLongStorage();
        store.seek(0);
        store.writeVLong(1);
        store.writeVLong(7);
        assertEquals(2, store.getPosition());
        store.writeVLong(777666555);
        assertEquals(7, store.getPosition());

        store.seek(0);
        assertEquals(1L, store.readVLong());
        assertEquals(7L, store.readVLong());
        assertEquals(777666555L, store.readVLong());
    }

    @Test
    public void testWriteWithTrim() {
        VLongStorage store = new VLongStorage();
        store.seek(0);
        store.writeVLong(1);
        store.trimToSize();
        assertEquals(1, store.getPosition());
        store.writeVLong(7);
        store.trimToSize();
        assertEquals(2, store.getPosition());
        store.writeVLong(777666555);
        store.trimToSize();
        assertEquals(7, store.getPosition());

        store.seek(0);
        assertEquals(1L, store.readVLong());
        assertEquals(7L, store.readVLong());
        assertEquals(777666555L, store.readVLong());
    }
}

@Test
public void testGrandesValeursNecessitantPlusieursOctets() {
    VLongStorage store = new VLongStorage();
    store.seek(0);
    
    // Teste différentes tailles de valeurs nécessitant différents nombres d'octets
    long val1 = 127L;          // 1 octet
    long val2 = 16383L;        // 2 octets
    long val3 = 2097151L;      // 3 octets
    long val4 = Long.MAX_VALUE; // 9 octets (valeur maximale)
    
    store.writeVLong(val1);
    store.writeVLong(val2);
    store.writeVLong(val3);
    store.writeVLong(val4);
    
    store.seek(0);
    assertEquals(val1, store.readVLong());
    assertEquals(val2, store.readVLong());
    assertEquals(val3, store.readVLong());
    assertEquals(val4, store.readVLong());
}

@Test
public void testObtenirOctetsEtLongueur() {
    VLongStorage store = new VLongStorage(20);
    
    // Vérifie la longueur initiale
    assertEquals(20, store.getLength());
    
    // Écrit quelques valeurs
    store.writeVLong(100);
    store.writeVLong(200);
    
    // Vérifie que getBytes() retourne le tableau
    byte[] bytes = store.getBytes();
    assertNotNull(bytes);
    assertEquals(20, bytes.length);
    
    // Après trim, la longueur devrait correspondre à la position
    store.trimToSize();
    assertEquals(store.getPosition(), store.getLength());
}

@Test
public void testNavigationEtManipulationPosition() {
    VLongStorage store = new VLongStorage();
    
    // Écrit des valeurs à différentes positions
    store.seek(0);
    store.writeVLong(111);
    long pos1 = store.getPosition();
    
    store.writeVLong(222);
    long pos2 = store.getPosition();
    
    store.writeVLong(333);
    
    // Retourne à la position intermédiaire et lit
    store.seek(pos1);
    assertEquals(222L, store.readVLong());
    
    // Retourne au début
    store.seek(0);
    assertEquals(111L, store.readVLong());
    assertEquals(222L, store.readVLong());
    assertEquals(333L, store.readVLong());
}

@Test
public void testExpansionTableau() {
    // Commence avec une petite capacité
    VLongStorage store = new VLongStorage(2);
    assertEquals(2, store.getLength());
    
    // Force l'expansion en écrivant plusieurs valeurs
    store.writeVLong(1);
    store.writeVLong(2);
    store.writeVLong(3);
    store.writeVLong(4);
    store.writeVLong(5);
    
    // Vérifie que le tableau s'est agrandi
    assertTrue(store.getLength() > 2);
    
    // Vérifie que toutes les valeurs sont lisibles
    store.seek(0);
    assertEquals(1L, store.readVLong());
    assertEquals(2L, store.readVLong());
    assertEquals(3L, store.readVLong());
    assertEquals(4L, store.readVLong());
    assertEquals(5L, store.readVLong());
}

@Test
public void testValeurZero() {
    VLongStorage store = new VLongStorage();
    
    // Teste l'écriture et la lecture de zéro
    store.seek(0);
    store.writeVLong(0L);
    assertEquals(1, store.getPosition()); // Devrait prendre 1 octet
    
    store.seek(0);
    assertEquals(0L, store.readVLong());
    
    // Teste avec d'autres valeurs autour de zéro
    store.writeVLong(1L);
    store.writeVLong(0L);
    store.writeVLong(100L);
    
    store.seek(1);
    assertEquals(1L, store.readVLong());
    assertEquals(0L, store.readVLong());
    assertEquals(100L, store.readVLong());
}