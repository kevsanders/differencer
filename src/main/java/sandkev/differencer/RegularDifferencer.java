package sandkev.differencer;

import sandkev.differencer.api.*;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;


public class RegularDifferencer<T extends Identifiable<K>,K>
  implements DiffAlgorithm<T,K,Comparator<? super T>,DiffComparator<? super T>> {

    private final Comparator<? super T> keyComparator;
    private final DiffComparator<? super T> dataComparator;

    public RegularDifferencer(Comparator<? super T> keyComparator,
                              DiffComparator<? super T> dataComparator) {
        this.keyComparator  = Objects.requireNonNull(keyComparator);
        this.dataComparator = Objects.requireNonNull(dataComparator);
    }

    @Override
    public void computeDiff(Iterable<T> expected,
                            Iterable<T> actual,
                            ComparisonResultHandler<T,K> handler) {
        Iterator<T> itE = expected.iterator();
        Iterator<T> itA = actual.iterator();
        T e = nextOrNull(itE), a = nextOrNull(itA);

        while (e != null && a != null) {
            int cmp = keyComparator.compare(a, e);
            if (cmp == 0) {
                DiffSummary d = dataComparator.compare(a, e);
                switch (d.getComparisonResult()) {
                  case Equal:
                    handler.onEqual(e.getId()); break;
                  case ApproximatelyEqual:
                    handler.onApproximatelyEqual(e.getId(), d); break;
                  default:
                    handler.onChanged(e.getId(), d); break;
                }
                a = nextOrNull(itA);
                e = nextOrNull(itE);
            } else if (cmp < 0) {
                handler.onAdded(a.getId(), a);
                a = nextOrNull(itA);
            } else {
                handler.onDropped(e.getId(), e);
                e = nextOrNull(itE);
            }
        }

        // flush remaining
        while (e != null) {
          handler.onDropped(e.getId(), e);
          e = nextOrNull(itE);
        }
        while (a != null) {
          handler.onAdded(a.getId(), a);
          a = nextOrNull(itA);
        }
    }

    private T nextOrNull(Iterator<T> it) {
        return (it != null && it.hasNext()) ? it.next() : null;
    }
}
