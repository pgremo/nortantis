package nortantis.util;

import org.apache.commons.collections4.bag.AbstractBagDecorator;
import org.apache.commons.collections4.bag.HashBag;

import java.util.Random;

public class Counter<T extends Comparable<T>> extends AbstractBagDecorator<T> {

    public Counter() {
        super(new HashBag<>());
    }

    public T sample(Random r) {
        return stream().skip(r.nextInt(size())).findFirst().get();
    }
}
