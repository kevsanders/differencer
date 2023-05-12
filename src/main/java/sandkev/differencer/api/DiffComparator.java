package sandkev.differencer.api;

import sandkev.differencer.DiffSummary;

public interface DiffComparator<T> {
    DiffSummary compare(T o1, T o2);
}
