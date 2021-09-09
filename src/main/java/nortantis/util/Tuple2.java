package nortantis.util;

/**
 * A 2-tuple of objects which are the same type and don't have to be comparable.
 */
public record Tuple2<F, S>(F first, S second) {
}
