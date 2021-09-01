package nortantis;

import nortantis.nlp.MarkovChain;
import nortantis.nlp.Syllables;
import nortantis.util.AssetsPath;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

import static java.lang.String.join;
import static java.nio.file.Files.readAllLines;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.generate;

public class NameGen {
    private final Random random;
    private final int max;
    private final int min;
    private final MarkovChain<String> chain = new MarkovChain<>();

    public NameGen(Random random, Path file, int min, int max) {
        this.random = random;
        this.max = max;
        this.min = min;

        try {
            readAllLines(file).stream()
                    .map(String::toLowerCase)
                    .map(Syllables::syllables)
                    .map(Arrays::stream)
                    .forEach(chain::addAll);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String create(int count) {
        return generate(() -> chain.randomWalk(random).limit(random.nextInt(max - min + 1) + min).collect(toList()))
                .filter(x -> x.size() >= min)
                .limit(count)
                .map(x -> join("", x))
                .collect(joining(" "));
    }

    public static void main(String[] args) {
        System.out.println(new NameGen(new Random(), AssetsPath.get("wordlists", "egyptian.txt"), 3, 5).create(1));
    }
}
