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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.github.javafaker.Faker;

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

    // 7 cas de tests fait par Bintou Touré et Ilona Nougbode

    /**
     * Cas 1 : Vérifie la lecture/écriture de grandes valeurs qui nécessite
     * plusieurs octets
     * Ce test s'assure que la méthode writeVLong() et readVLong() peuvent gérer
     * correctement
     * des entiers de différentes tailles
     */

    @Test
    public void testGrandesValeursNecessitantPlusieursOctets() {
        VLongStorage store = new VLongStorage();
        store.seek(0);

        // Teste les différentes tailles de valeurs qui on besoin d'un différents
        // d'octets
        long val1 = 127L; // 1 octet
        long val2 = 16383L; // 2 octets
        long val3 = 2097151L; // 3 octets
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

    /**
     * Cas 2 : Vérifie la gestion des octets internes et la longueur du tableau
     * Le but de ce test est de s'assurer que getLength() et getBytes() fonctionnent
     * correctement avant et après
     * l’écriture de données, et que trimToSize() ajuste la taille du tableau selon
     * la position actuelle.
     */
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

    /**
     * Cas 3 : Vérifie la navigation et la manipulation de la position.
     * On veut que le test contrôle seek(), getPosition(), writeVLong() et
     * readVLong() pour qu'ils
     * permettent de naviguer correctement dans les données et de lire les bonnes
     * valeurs après avoir
     * déplacer le curseur à des positions
     */
    @Test
    public void testNavigationEtManipulationPosition() {
        VLongStorage store = new VLongStorage();

        // Écrit des valeurs à différentes positions
        store.seek(0);
        store.writeVLong(111);
        long pos1 = store.getPosition();

        store.writeVLong(222);
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

    /**
     * Cas 4 : Vérifie l’expansion automatique du tableau interne
     * On commence avec une petite capacité et écrit plusieurs valeurs
     * pour vérifier que le tableau interne grandit.
     */
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

        // Vérifie si le tableau s'est agrandi
        assertTrue(store.getLength() > 2);

        // Vérifie que toutes les valeurs sont lisibles
        store.seek(0);
        assertEquals(1L, store.readVLong());
        assertEquals(2L, store.readVLong());
        assertEquals(3L, store.readVLong());
        assertEquals(4L, store.readVLong());
        assertEquals(5L, store.readVLong());
    }

    /**
     * Cas 5 : Vérifie la gestion de la valeur zéro et les cas voisins
     * Ce test s’assure que la valeur 0 est correctement écrite et lu,
     * et que les valeurs autour de zéro sont également bien encodées
     */
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

    /**
     * Cas 6: Vérifie que le constructeur prends un tableau existant et
     * s’assure que le constructeur VLongStorage(byte[]) initialise correctement
     * la longueur et la position du stockage avec le tableau fourni
     */
    @Test
    public void testConstructeurAvecTableauExistant() {
        // Teste le constructeur avec un tableau pré-existant
        byte[] existingBytes = new byte[] { 1, 2, 3, 4, 5 };
        VLongStorage store = new VLongStorage(existingBytes);

        assertEquals(5, store.getLength());
        assertEquals(0, store.getPosition());
    }

    /**
     * Cas 7 : Couvre toutes les branches de lecture dans readVLong()
     * Le but de ce test est d'utiliser des valeurs spécifiques pour activer tous
     * les cas de lecture.
     */
    @Test
    public void testReadVLongToutesLesBranches() {
        VLongStorage store = new VLongStorage();

        // Teste toutes les branches du readVLong (valeurs spécifiques pour couvrir
        // chaque if)
        store.writeVLong(0L); // b >= 0 immédiatement
        store.writeVLong(127L); // b >= 0 immédiatement (max 1 byte)
        store.writeVLong(128L); // Entre dans 2ème byte, b >= 0
        store.writeVLong(16383L); // 2 bytes complets
        store.writeVLong(16384L); // Entre dans 3ème byte
        store.writeVLong(2097151L); // 3 bytes complets
        store.writeVLong(268435455L); // 4 bytes
        store.writeVLong(34359738367L); // 5 bytes

        store.seek(0);
        assertEquals(0L, store.readVLong());
        assertEquals(127L, store.readVLong());
        assertEquals(128L, store.readVLong());
        assertEquals(16383L, store.readVLong());
        assertEquals(16384L, store.readVLong());
        assertEquals(2097151L, store.readVLong());
        assertEquals(268435455L, store.readVLong());
        assertEquals(34359738367L, store.readVLong());
    }
    /** Test java-faker. Le but de ce test est d'utiliser java-Faker nous voulons
    * tester avec des données aléatoires qui vont être générées par java-faker. 
    * On vérifie si VLongStorage peut gérer un grand volume de valeurs aléatoires de différentes tailles. */
}
