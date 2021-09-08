package nortantis.nlp;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;

public class Syllables {
    private static final Pattern splitter = compile("-+", CASE_INSENSITIVE);

    private static final List<Pattern> patterns = List.of(
            compile("\\W+", CASE_INSENSITIVE),
            compile("(?=[aeiou](?:tch|ch|ph|sh|th|wh|zh|[a-z&&[^aeiou]])[aeiou])([aeiou])(tch|ch|ph|sh|th|wh|zh|[a-z&&[^aeiou]])", CASE_INSENSITIVE),
            compile("(?=[aeiou][a-z&&[^aeiou]][a-z&&[^aeiou]][aeiou])([aeiou][a-z&&[^aeiou]])([a-z&&[^aeiou]])", CASE_INSENSITIVE),
            compile("(?=[aeiou][a-z&&[^aeiou]]{3}[aeiou])([aeiou](?:tch|ch|ph|sh|th|wh|zh|[a-z&&[^aeiou]]))([a-z&&[^aeiou]]{1,2})", CASE_INSENSITIVE),
            compile("(?=[aeiou][a-z&&[^aeiou]]{4}[aeiou])([aeiou][a-z&&[^aeiou]]{2})([a-z&&[^aeiou]]{2})", CASE_INSENSITIVE)
    );

    public static Stream<String> syllables(String word) {
        return splitter.splitAsStream(patterns.stream().reduce(word, (acc, pair) -> pair.matcher(acc).replaceAll("$1-$2"), (a, b) -> a + b));
    }
}
