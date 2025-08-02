package sandkev.differencer;

import lombok.Getter;
import lombok.ToString;
import sandkev.differencer.api.ComparisonResultHandler;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@ToString(onlyExplicitlyIncluded = true)
public class ComparisonResultStats<T,K> implements ComparisonResultHandler<T,K> {
    @Getter @ToString.Include
    private final AtomicInteger equalCount = new AtomicInteger();
    @Getter @ToString.Include
    private final AtomicInteger approximatelyEqualCount = new AtomicInteger();
    @Getter @ToString.Include
    private final AtomicInteger addedCount = new AtomicInteger();
    @Getter @ToString.Include
    private final AtomicInteger droppedCount = new AtomicInteger();
    @Getter @ToString.Include
    private final AtomicInteger changedCount = new AtomicInteger();

    private final Set<K> changedKeys = ConcurrentHashMap.newKeySet();
    private final Set<K> addedKeys   = ConcurrentHashMap.newKeySet();
    private final Set<K> droppedKeys = ConcurrentHashMap.newKeySet();

    @Override
    public void onEqual(K id) {
        equalCount.incrementAndGet();
    }

    @Override
    public void onApproximatelyEqual(K id, DiffSummary diff) {
        approximatelyEqualCount.incrementAndGet();
    }

    @Override
    public void onAdded(K id, T added) {
        addedKeys.add(id);
        addedCount.incrementAndGet();
    }

    @Override
    public void onDropped(K id, T dropped) {
        droppedKeys.add(id);
        droppedCount.incrementAndGet();
    }

    @Override
    public void onChanged(K id, DiffSummary diff) {
        changedKeys.add(id);
        changedCount.incrementAndGet();
    }

    // Unmodifiable views to protect internal state
    public Set<K> getChangedKeys() {
        return Collections.unmodifiableSet(changedKeys);
    }

    public Set<Object> getAddedKeys() {
        return Collections.unmodifiableSet(addedKeys);
    }

    public Set<Object> getDroppedKeys() {
        return Collections.unmodifiableSet(droppedKeys);
    }

    // Optionally, a reset method
    public void reset() {
        equalCount.set(0);
        approximatelyEqualCount.set(0);
        addedCount.set(0);
        droppedCount.set(0);
        changedCount.set(0);
        changedKeys.clear();
        addedKeys.clear();
        droppedKeys.clear();
    }
}
