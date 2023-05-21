package sandkev.differencer;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import sandkev.differencer.api.*;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Created by kevin on 08/10/2018.
 */
@Slf4j
public class RegularDifferencerTest {

    @Data
    @Builder(toBuilder = true)
    public static class MyTypeKey {
        String name;
        Long domain;
    }
    @Data
    @Builder(toBuilder = true)
    public static class MyType implements Identifiable<MyTypeKey> {
        String name;
        Long domain;
        int region;
        int flavour;
        String bio;
        LocalDateTime lastUpdated;
        @Override
        public MyTypeKey getId() {
            return MyTypeKey.builder()
                    .name(name)
                    .domain(domain)
                    .build();
        }
    }

    @Test
    public void canCompareMatchedSortedSet(){

        Comparator<MyType> keyComparator = Comparator
                .comparing(MyType::getDomain)
                .thenComparing(MyType::getName);

        DiffComparator<MyType> dataComparator = (o1, o2) -> {
            DiffSummary builder = new DiffSummary();
            if (o1.getFlavour() != o2.getFlavour()) {
                builder.addDiff("flavour",
                        o1.getFlavour(), o2.getFlavour(),
                        ComparisonResult.Changed);
            }
            if (o1.getRegion() != o2.getRegion()) {
                builder.addDiff("region",
                        o1.getRegion(), o2.getRegion(),
                        ComparisonResult.Changed);
            }
            if (!Objects.equals(o1.getBio(), o2.getBio())) {
                builder.addDiff("bio",
                        o1.getBio(), o2.getBio(),
                        ComparisonResult.Changed);
            }
            return builder;
        };

        TreeSet<MyType> originals = new TreeSet<>(keyComparator);
        originals.add(MyType.builder().domain(1L).name("a").region(0).flavour(1).bio("cool").build());
        originals.add(MyType.builder().domain(1L).name("b").region(0).flavour(1).bio("cool").build());
        originals.add(MyType.builder().domain(1L).name("c").region(0).flavour(1).bio("cool").build());
        originals.add(MyType.builder().domain(1L).name("d").region(0).flavour(1).bio("cool").build());
        originals.add(MyType.builder().domain(1L).name("e").region(0).flavour(1).bio("cool").build());

        TreeSet<MyType> revisions = new TreeSet<>(keyComparator);
        revisions.add(MyType.builder().domain(1L).name("a").region(0).flavour(1).bio("cool").build());
        revisions.add(MyType.builder().domain(1L).name("b").region(0).flavour(1).bio("cool").build());
        revisions.add(MyType.builder().domain(1L).name("c").region(0).flavour(1).bio("cool").build());
        revisions.add(MyType.builder().domain(1L).name("d").region(1).flavour(1).bio("cool").build());
        revisions.add(MyType.builder().domain(1L).name("e").region(0).flavour(2).bio("coool").build());


        DiffAlgorithm<MyType, MyTypeKey, Comparator<MyType>, DiffComparator<MyType>, Iterable<MyType>> differencer
                = new RegularDifferencer(keyComparator, dataComparator);

        AtomicInteger equalCount = new AtomicInteger();
        AtomicInteger approximatelyEqualCount = new AtomicInteger();
        AtomicInteger addedCount = new AtomicInteger();
        AtomicInteger droppedCount = new AtomicInteger();
        AtomicInteger changedCount = new AtomicInteger();
        ComparisonResultHandler<MyType,MyTypeKey> handler = new ComparisonResultHandler<MyType, MyTypeKey>() {
            @Override
            public void onEqual(MyTypeKey id) {
                log.info("equals: " + id);
                equalCount.incrementAndGet();
            }

            @Override
            public void onApproximatelyEqual(MyTypeKey id, DiffSummary diff) {
                log.info("appoximatelyEquals: " + id + " diff: " + diff);
                approximatelyEqualCount.incrementAndGet();
            }

            @Override
            public void onAdded(MyTypeKey id, MyType added) {
                log.info("added: " + added );
                addedCount.incrementAndGet();
            }

            @Override
            public void onDropped(MyTypeKey id, MyType dropped) {
                log.info("dropped: " + dropped );
                droppedCount.incrementAndGet();
            }

            @Override
            public void onChanged(MyTypeKey id, DiffSummary diff) {
                log.info("changed: " + id + " diff: " + diff );
                changedCount.incrementAndGet();
            }
        };
        Supplier<Iterable<MyType>> expectedSource = () -> originals;
        Supplier<Iterable<MyType>> actualSource = () -> revisions;
        differencer.computeDiff(expectedSource, actualSource, handler);

        log.info("equalCount: " + equalCount + ", approximatelyEqualCount: " + approximatelyEqualCount +
                ", addedCount: " + addedCount + ", droppedCount: " + droppedCount + ", changedCount: " +changedCount);


    }

}