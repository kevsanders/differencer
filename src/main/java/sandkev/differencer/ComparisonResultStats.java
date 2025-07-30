package sandkev.differencer;

import sandkev.differencer.api.ComparisonResultHandler;

import java.util.concurrent.atomic.AtomicInteger;

public class ComparisonResultStats implements ComparisonResultHandler {
    private final AtomicInteger equalCount = new AtomicInteger();
    private final AtomicInteger approximatelyEqualCount = new AtomicInteger();
    private final AtomicInteger addedCount = new AtomicInteger();
    private final AtomicInteger droppedCount = new AtomicInteger();
    private final AtomicInteger changedCount = new AtomicInteger();

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
        addedCount.incrementAndGet();
    }

    @Override
    public void onDropped(Object id, Object dropped) {
        droppedCount.incrementAndGet();
    }

    @Override
    public void onChanged(Object id, DiffSummary diff) {
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
}
