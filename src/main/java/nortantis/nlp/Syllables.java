package nortantis.nlp;

import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;
import static org.apache.commons.lang3.tuple.ImmutablePair.of;

public class Syllables {
    private static final Pattern splitter = compile("-+", CASE_INSENSITIVE);

    private static final List<Pair<Pattern, String>> patterns = List.of(
            of(compile("\\W+", CASE_INSENSITIVE), "-"),
            of(compile("(?=[aeiou](?:tch|ch|ph|sh|th|wh|zh|[a-z&&[^aeiou]])[aeiou])([aeiou])(tch|ch|ph|sh|th|wh|zh|[a-z&&[^aeiou]])", CASE_INSENSITIVE), "$1-$2"),
            of(compile("(?=[aeiou][a-z&&[^aeiou]][a-z&&[^aeiou]][aeiou])([aeiou][a-z&&[^aeiou]])([a-z&&[^aeiou]])", CASE_INSENSITIVE), "$1-$2"),
            of(compile("(?=[aeiou][a-z&&[^aeiou]]{3}[aeiou])([aeiou](tch|ch|ph|sh|th|wh|zh|[a-z&&[^aeiou]]))([a-z&&[^aeiou]]{1,2})", CASE_INSENSITIVE), "$1-$3"),
            of(compile("(?=[aeiou][a-z&&[^aeiou]]{4}[aeiou])([aeiou][a-z&&[^aeiou]]{2})([a-z&&[^aeiou]]{2})", CASE_INSENSITIVE), "$1-$2")
    );

    public static String[] syllables(String word) {
        return splitter.split(patterns.stream().reduce(word, (acc, pair) -> pair.getLeft().matcher(acc).replaceAll(pair.getRight()), (a, b) -> a));
    }
}
