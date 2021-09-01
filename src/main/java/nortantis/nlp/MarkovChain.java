package nortantis.nlp;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class MarkovChain<E> {
    private final Map<E, Bag<E>> items = new HashMap<>();

    public void addAll(Stream<E> xs) {
        add(xs.reduce(null, (acc, i) -> {
            add(acc, i);
            return i;
        }), null);
    }

    public void addAll(Stream<E> xs, int count) {
        addMany(xs.reduce(null, (acc, x) -> {
            addMany(acc, x, count);
            return x;
        }), null, count);
    }

    public void addMany(E current, E next, int count) {
        items.computeIfAbsent(current, k -> new HashBag<>()).add(next, count);
    }

    public void add(E current, E next) {
        items.computeIfAbsent(current, k -> new HashBag<>()).add(next);
    }

    public Stream<E> randomWalk(Random random) {
        UnaryOperator<E> next = (E e) -> {
            Bag<E> es = items.get(e);
            return es == null ? null : es.stream()
                    .skip(random.nextInt(es.size()))
                    .iterator().next();
        };
        return Stream.iterate(next.apply(null), Objects::nonNull, next);
    }
}
