package nortantis.util;

import org.apache.commons.collections4.bag.AbstractBagDecorator;
import org.apache.commons.collections4.bag.HashBag;

import java.util.Random;

public class Counter<T extends Comparable<T>> extends AbstractBagDecorator<T> {

    public Counter() {
        super(new HashBag<>());
    }

    public Counter(T... items){
        this();
        addAll(items);
    }

    public void addAll(T... items) {
        for (var item : items) add(item);
    }

    public T random(Random r) {
        return stream().skip(r.nextInt(size())).findFirst().orElse(null);
    }
}
