package nortantis.util;

/**
 * A 2-tuple of objects which don't have to be comparable. For a comparable version of this, see Pair.
 * @author joseph
 *
 */
public record Tuple3<F, S, T> (F first, S second, T third){
}
