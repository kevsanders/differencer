package sandkev.differencer.api;

import java.util.Comparator;
import java.util.function.Supplier;

/**
 * @param <T> Type of object to be compared
 * @param <K> Primary key of object to be compared
 * @param <C> Key comparator
 * @param <D> Data comparator
 */
public interface DiffAlgorithm<T extends Identifiable<K>, K, C extends Comparator<? super T>, D extends DiffComparator<? super T>> {
    void computeDiff(Iterable<T> expectedSource, Iterable<T> actualSource, ComparisonResultHandler<T,K> handler);
}
