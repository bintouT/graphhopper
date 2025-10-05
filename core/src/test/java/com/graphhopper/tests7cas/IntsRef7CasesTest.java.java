package com.graphhopper.tests7cas;

import com.graphhopper.storage.IntsRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("IntsRef – Tests 7 Cas")
class IntsRef7CasesTest {

    /**
     * Cas 1 — hashCode : deux IntsRef de même contenu sont égaux et partagent le
     * même hashCode.
     */
    @Test
    @DisplayName("equals/hashCode – contenu identique")
    void equalsAndHashCode_consistent() {
        IntsRef a = new IntsRef(new int[] { 1, 2, 3 }, 0, 3);
        IntsRef b = new IntsRef(new int[] { 1, 2, 3 }, 0, 3);
        IntsRef c = new IntsRef(new int[] { 1, 2, 4 }, 0, 3);

        assertEquals(a, b, "Deux IntsRef au même contenu doivent être égaux");
        assertEquals(a.hashCode(), b.hashCode(), "hashCode doit être identique pour des objets égaux");
        assertNotEquals(a, c, "Une différence d’un élément doit annuler l’égalité");
    }

    /** Cas 2 — compareTo : compare les éléments selon l’ordre alphabétique */
    @Test
    @DisplayName("compareTo – (2<3)")
    void compareTo_lexicographic_lessThan() {
        IntsRef a = new IntsRef(new int[] { 1, 2 }, 0, 2);
        IntsRef b = new IntsRef(new int[] { 1, 3 }, 0, 2);

        assertTrue(a.compareTo(b) < 0, "a doit être plus petit que b (2 < 3)");
        assertTrue(b.compareTo(a) > 0, "b doit être plus grand que a (3 > 2)");
    }

    /** Cas 3 — compareTo : règle du préfixe plus court < plus long */
    @Test
    @DisplayName("compareTo – règle du préfixe plus court < plus long")
    void compareTo_prefixRule() {
        IntsRef shortRef = new IntsRef(new int[] { 7, 8 }, 0, 2);
        IntsRef longRef = new IntsRef(new int[] { 7, 8, 0 }, 0, 3);

        assertTrue(shortRef.compareTo(longRef) < 0, "Le préfixe plus court doit être classé avant");
        IntsRef sameAsShort = new IntsRef(new int[] { 7, 8 }, 0, 2);
        assertEquals(0, shortRef.compareTo(sameAsShort), "Deux séquences identiques doivent être égales");
    }

    /** Cas 4 — deepCopyOf et toString */
    @Test
    @DisplayName("deepCopyOf + toString")
    void deepCopyOf_preservesContent_and_toString_notEmpty() {
        IntsRef orig = new IntsRef(new int[] { 9, 10, 11 }, 0, 3);
        IntsRef cp = IntsRef.deepCopyOf(orig);

        assertTrue(orig.intsEquals(cp), "La copie doit avoir exactement du même contenu");
        assertEquals(orig, cp, "equals doit aussi être égale");
        assertNotSame(orig, cp, "deepCopyOf doit renvoyer une nouvelle instance");

        String s1 = orig.toString();
        String s2 = cp.toString();
        assertNotNull(s1);
        assertFalse(s1.isEmpty(), "toString() ne doit pas être vide");
        assertNotNull(s2);
        assertFalse(s2.isEmpty(), "toString() ne doit pas être vide");
    }

    /** Cas 5 — isEmpty : vrai pour longueur nulle, faux sinon */
    @Test
    @DisplayName("isEmpty – vrai si longueur 0, faux sinon")
    void isEmpty_behaviour() {
        IntsRef empty = new IntsRef(new int[0], 0, 0);
        assertTrue(empty.isEmpty(), "Un IntsRef de longueur 0 doit être vide");

        IntsRef nonEmpty = new IntsRef(new int[] { 1, 2 }, 0, 2);
        assertFalse(nonEmpty.isEmpty(), "Un IntsRef de longueur > 0 ne doit pas être vide");
    }

    /** Cas 6 — equals: false quand on compare à null ou un autre type 
    @Test
    @DisplayName("equals – false pour null ou autre type d'objet")
    void testEqualsWithNullAndDifferentType() {
        IntsRef ref = new IntsRef(new int[] { 1, 2, 3 }, 0, 3);
        assertNotEquals(ref, null, "equals doit renvoyer false pour null");
        assertNotEquals(ref, "pas un IntsRef", "equals doit renvoyer false pour une autre classe");
    }

    /** Cas 7 — isValid: false quand offset/longueur non valides 
    @Test
    @DisplayName("isValid – false quand offset/longueur non valides")
    void testIsValidForInvalidRefs() {
        IntsRef validRef = new IntsRef(new int[] { 1, 2, 3 }, 0, 3);
        assertTrue(validRef.isValid(), "Un IntsRef normal devrait être valide");

        IntsRef invalidRef = new IntsRef(new int[] { 1, 2, 3 }, 2, 5);
        assertFalse(invalidRef.isValid(), "isValid doit renvoyer false pour une combinaison invalide");
    }*/
}
