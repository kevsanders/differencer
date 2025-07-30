package sandkev.differencer;

import sandkev.differencer.api.ComparisonResultHandler;
import sandkev.differencer.api.DiffAlgorithm;
import sandkev.differencer.api.DiffComparator;
import sandkev.differencer.api.Identifiable;

import java.util.Comparator;
import java.util.Iterator;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;


/**
 * A one‑pass differ for two sorted, duplicate‑free streams of Identifiable<T>.
 *
 * <p><strong>Preconditions:</strong>
 * <ul>
 *   <li>Each source Iterable must be sorted strictly by the provided keyComparator.</li>
 *   <li>No two items in a source may be “equal” under that same comparator.</li>
 * </ul>
 */
public class RegularDifferencer<T extends Identifiable<K>,K>
  implements DiffAlgorithm<T,K,Comparator<? super T>,DiffComparator<? super T>> {

    private final Comparator<? super T> keyComparator;
    private final DiffComparator<? super T> dataComparator;
    /**
     * @param validateInputs if true, wraps each input in a ValidatingIterable that
     *                       enforces sortedness & uniqueness at iteration time.
     */
    private final boolean validateInputs;

    public RegularDifferencer(Comparator<? super T> keyComparator,
                              DiffComparator<? super T> dataComparator) {
        this(keyComparator, dataComparator, false);
    }
    /**
     * @param keyComparator   used to order and identify equality of T instances
     * @param dataComparator  used to compute field‑level diffs once keys match
     * @param validateInputs  if true, wrap inputs in a ValidatingIterable to enforce sortedness & uniqueness
     */
    public RegularDifferencer(Comparator<? super T> keyComparator,
                              DiffComparator<? super T> dataComparator, boolean validateInputs) {
        this.keyComparator  = requireNonNull(keyComparator);
        this.dataComparator = requireNonNull(dataComparator);
        this.validateInputs = validateInputs;
    }

    public static <T extends Identifiable<K>,K> RegularDifferencer<T,K> withValidation(Comparator<? super T> keyComparator,
                                                                                       DiffComparator<? super T> dataComparator) {
       return new RegularDifferencer<>(keyComparator, dataComparator, true);
    }
    public static <T extends Identifiable<K>,K> RegularDifferencer<T,K> withoutValidation(Comparator<? super T> keyComparator,
                                                                                          DiffComparator<? super T> dataComparator) {
        return new RegularDifferencer<>(keyComparator, dataComparator, false);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if validation is on and inputs are out‑of‑order or contain duplicates
     */
    @Override
    public void computeDiff(Iterable<T> expected,
                            Iterable<T> actual,
                            ComparisonResultHandler<T,K> handler) {

        requireNonNull(expected, "expected iterable must not be null");
        requireNonNull(actual,   "actual iterable must not be null");
        requireNonNull(handler,  "handler must not be null");

        //optionally decorate the iterators with validation that they are sorted sets
        Iterable<T> expectedIterable = validateInputs
                ? new ValidatingIterable<>(expected, keyComparator)
                : expected;
        Iterable<T> actualIterable   = validateInputs
                ? new ValidatingIterable<>(actual,   keyComparator)
                : actual;
        final Iterator<T> itE = expectedIterable.iterator();
        final Iterator<T> itA = actualIterable.iterator();

        T e = poll(itE), a = poll(itA);

        while (e != null && a != null) {
            int cmp = keyComparator.compare(a, e);
            if (cmp == 0) {
                handleMatch(handler, e, a);
                a = poll(itA);
                e = poll(itE);
            } else if (cmp < 0) {
                handler.onAdded(a.getId(), a);
                a = poll(itA);
            } else {
                handler.onDropped(e.getId(), e);
                e = poll(itE);
            }
        }

        // flush remaining
        if (e != null) {
            handler.onDropped(e.getId(), e);
            itE.forEachRemaining(drop(handler));
        }
        if (a != null) {
            handler.onAdded(a.getId(), a);
            itA.forEachRemaining(add(handler));
        }
    }

    private void handleMatch(ComparisonResultHandler<T, K> handler, T e, T a) {
        DiffSummary d = dataComparator.compare(a, e);
        switch (d.getComparisonResult()) {
          case Equal:
            handler.onEqual(e.getId()); break;
          case ApproximatelyEqual:
            handler.onApproximatelyEqual(e.getId(), d); break;
          case Changed:
            handler.onChanged(e.getId(), d); break;
            // no default → unhandled new enum values will be a compile‑time error
        }
    }

    private static <T> T poll(Iterator<T> it) {
        return it.hasNext() ? it.next() : null;
    }
    private Consumer<T> drop(ComparisonResultHandler<T,K> h ) {
        return rem -> h.onDropped(rem.getId(), rem);
    }
    private Consumer<T> add( ComparisonResultHandler<T,K> h ) {
        return rem -> h.onAdded(rem.getId(), rem);
    }

    boolean isValidationOn() {
        return validateInputs;
    }
}
