package sandkev.differencer;

import lombok.Getter;
import sandkev.differencer.api.ComparisonResultHandler;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class ComparisonResultStats implements ComparisonResultHandler {
    @Getter
    private final AtomicInteger equalCount = new AtomicInteger();
    @Getter
    private final AtomicInteger approximatelyEqualCount = new AtomicInteger();
    @Getter
    private final AtomicInteger addedCount = new AtomicInteger();
    @Getter
    private final AtomicInteger droppedCount = new AtomicInteger();
    @Getter
    private final AtomicInteger changedCount = new AtomicInteger();
    private Set<Object> changedKeys = new HashSet<>();
    private Set<Object> addedKeys = new HashSet<>();
    private Set<Object> droppedKeys = new HashSet<>();

    @Override
    public void onEqual(Object id) {
        equalCount.incrementAndGet();
    }

    @Override
    public void onApproximatelyEqual(Object id, DiffSummary diff) {
        approximatelyEqualCount.incrementAndGet();
    }

    @Override
    public void onAdded(Object id, Object added) {
        addedKeys.add(id);
        addedCount.incrementAndGet();
    }

    @Override
    public void onDropped(Object id, Object dropped) {
        droppedKeys.add(id);
        droppedCount.incrementAndGet();
    }

    @Override
    public void onChanged(Object id, DiffSummary diff) {
        changedKeys.add(id);
        changedCount.incrementAndGet();
    }

    @Override
    public String describe() {
        return toString();
    }

    @Override
    public String toString() {
        return "ComparisonResultStats{" +
                "equal=" + equalCount +
                ", approximatelyEqual=" + approximatelyEqualCount +
                ", added=" + addedCount +
                ", dropped=" + droppedCount +
                ", changed=" + changedCount +
                '}';
    }

    public Set<Object> getChangedKeys() {
        return changedKeys;
    }

    public Set<Object> getAddedKeys() {
        return addedKeys;
    }

    public Set<Object> getDroppedKeys() {
        return droppedKeys;
    }

}
