package sandkev.differencer.api;

import java.util.Comparator;
import java.util.function.Supplier;

/**
 * @param <T> Type of object to be compared
 * @param <K> Primary key of object to be compared
 * @param <C> Key comparator
 * @param <D> Data comparator
 * @param <I> Iterable data input source
 */
public interface DiffAlgorithm<T extends Identifiable<K>, K, C extends Comparator<T>, D extends DiffComparator<T>, I extends Iterable<T>> {
    void computeDiff(Supplier<I> expectedSource, Supplier<I> actualSource, ComparisonResultHandler handler);
}
