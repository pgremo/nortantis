package nortantis.nlp;

import org.junit.Test;

import static nortantis.nlp.Syllables.syllables;
import static org.junit.Assert.assertArrayEquals;

public class SyllablesTest {
    @Test
    public void testSplitComplexWord() {
        var expected = new String[]{"he", "gan", "shab", "but", "tesh"};
        var actual = syllables("heganshabbuttesh").toArray(String[]::new);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testSplitSingleConsonant() {
        var expected = new String[]{"me", "ku"};
        var actual = syllables("meku").toArray(String[]::new);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testSplitMultipleSingleConsonants() {
        var expected = new String[]{"ma", "sa", "tei", "pe"};
        var actual = syllables("masateipe").toArray(String[]::new);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testSplitDoubleConsonantsAtEnd() {
        var expected = new String[]{"matt"};
        var actual = syllables("matt").toArray(String[]::new);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testSplitDoubleConsonants() {
        var expected = new String[]{"ob", "re", "gon"};
        var actual = syllables("obregon").toArray(String[]::new);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testSplitTripleConsonants() {
        var expected = new String[]{"bol", "chun"};
        var syllables = syllables("bolchun").toArray(String[]::new);
        var actual = syllables;
        assertArrayEquals(expected, actual);

        expected = new String[]{"bol", "vlun"};
        syllables = syllables("bolvlun").toArray(String[]::new);
        actual = syllables;
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testSplitQuadrupleConsonants() {
        var expected = new String[]{"kalt", "glon"};
        var actual = syllables("kaltglon").toArray(String[]::new);
        assertArrayEquals(expected, actual);
    }
}