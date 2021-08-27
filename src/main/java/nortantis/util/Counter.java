package nortantis.util;

import org.apache.commons.collections4.bag.AbstractBagDecorator;
import org.apache.commons.collections4.bag.HashBag;

import java.util.Random;

public class Counter<T extends Comparable<T>> extends AbstractBagDecorator<T> {

    public Counter() {
        super(new HashBag<>());
    }

    public T sample(Random r) {
        var uniformSample = r.nextInt(size());

        double acc = 0;
        for (var item : uniqueSet()) {
            acc += getCount(item);
            if (acc >= uniformSample)
                return item;
        }

        throw new IllegalStateException("unable to find sample");
    }
}
