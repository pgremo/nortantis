package nortantis;

import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

class ListValuedMapCollector<T, A, R> implements Collector<T, A, R> {
    static <T, K, U> ListValuedMapCollector<T, ?, ListValuedMap<K, U>> toListValuedMap(Function<? super T, ? extends K> getKey, Function<? super T, ? extends U> getValue) {
        return new ListValuedMapCollector<>(
                ArrayListValuedHashMap::new,
                (map, element) -> {
                    K k = getKey.apply(element);
                    U v = getValue.apply(element);
                    map.put(k, v);
                },
                (a, b) -> {
                    a.putAll(b);
                    return a;
                },
                Function.identity(),
                Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH))
        );
    }

    static <T, K, U, M extends ListValuedMap<K, U>> ListValuedMapCollector<T, ?, M> toListValuedMap(
            Function<? super T, ? extends K> getKey,
            Function<? super T, ? extends U> getValue,
            Supplier<M> mapFactory) {
        return new ListValuedMapCollector<>(
                mapFactory,
                (map, element) -> {
                    K k = getKey.apply(element);
                    U v = getValue.apply(element);
                    map.put(k, v);
                },
                (a, b) -> {
                    a.putAll(b);
                    return a;
                },
                Function.identity(),
                Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH))
        );
    }

    private final Supplier<A> supplier;
    private final BiConsumer<A, T> accumulator;
    private final BinaryOperator<A> combiner;
    private final Function<A, R> finisher;
    private final Set<Characteristics> characteristics;

    ListValuedMapCollector(Supplier<A> supplier,
                           BiConsumer<A, T> accumulator,
                           BinaryOperator<A> combiner,
                           Function<A, R> finisher,
                           Set<Characteristics> characteristics) {
        this.supplier = supplier;
        this.accumulator = accumulator;
        this.combiner = combiner;
        this.finisher = finisher;
        this.characteristics = characteristics;
    }

    @Override
    public BiConsumer<A, T> accumulator() {
        return accumulator;
    }

    @Override
    public Supplier<A> supplier() {
        return supplier;
    }

    @Override
    public BinaryOperator<A> combiner() {
        return combiner;
    }

    @Override
    public Function<A, R> finisher() {
        return finisher;
    }

    @Override
    public Set<Characteristics> characteristics() {
        return characteristics;
    }
}
