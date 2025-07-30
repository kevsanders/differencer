package sandkev.differencer;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import sandkev.differencer.api.*;

import java.time.LocalDateTime;
import java.util.*;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;


/**
 * Created by kevin on 08/10/2018.
 */
@Slf4j
public class RegularDifferencerTest {

    private Comparator<MyType> keyComparator;
    private DiffComparator<MyType> dataComparator;
    private DiffAlgorithm<MyType, MyTypeKey, Comparator<MyType>, DiffComparator<MyType>> differencer;
    private ComparisonResultStats stats;
    private ComparisonResultHandler<MyType, MyTypeKey> handler;

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

    @Before
    public void setUp() {
        keyComparator = Comparator.comparing(MyType::getDomain)
                .thenComparing(MyType::getName);
        dataComparator = (o1, o2) -> {
            DiffSummary diffs = new DiffSummary();
            if (o1.getFlavour() != o2.getFlavour()) {
                diffs.addDiff("flavour",
                        o1.getFlavour(), o2.getFlavour(),
                        ComparisonResult.Changed);
            }
            if (o1.getRegion() != o2.getRegion()) {
                diffs.addDiff("region",
                        o1.getRegion(), o2.getRegion(),
                        ComparisonResult.Changed);
            }
            if (!Objects.equals(o1.getBio(), o2.getBio())) {
                diffs.addDiff("bio",
                        o1.getBio(), o2.getBio(),
                        ComparisonResult.Changed);
            }
            return diffs;
        };

        differencer = new RegularDifferencer(keyComparator, dataComparator);
        stats = new ComparisonResultStats();
        handler = new ComparisonResultHandler<MyType, MyTypeKey>() {
            @Override
            public void onEqual(MyTypeKey id) {
                log.info("equals: " + id);
                stats.onEqual(id);
            }

            @Override
            public void onApproximatelyEqual(MyTypeKey id, DiffSummary diff) {
                log.info("appoximatelyEquals: " + id + " diff: " + diff);
                stats.onApproximatelyEqual(id, diff);
            }

            @Override
            public void onAdded(MyTypeKey id, MyType added) {
                log.info("added: " + added );
                stats.onAdded(id, added);
            }

            @Override
            public void onDropped(MyTypeKey id, MyType dropped) {
                log.info("dropped: " + dropped );
                stats.onDropped(id, dropped);
            }

            @Override
            public void onChanged(MyTypeKey id, DiffSummary diff) {
                log.info("changed: " + id + " diff: " + diff );
                stats.onChanged(id, diff);
            }

            @Override
            public String describe() {
                return stats.describe();
            }

        };

    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsOnUnsortedInputWhenValidateOn() {
        List<MyType> unsorted = Arrays.asList(
                make(1L,"b", 0, 1, "cool"),
                make(1L,"a", 0, 1, "cool")
        );
        RegularDifferencer<MyType,MyTypeKey> vDiff =
                new RegularDifferencer<>(keyComparator, dataComparator, true);
        vDiff.computeDiff(unsorted, unsorted, handler);
    }

    @Test
    public void testTwoChangesNoAddsOrDrops() {
        // arrange
        List<MyType> original = Arrays.asList(
                make(1L,"a",0,1,"cool"),
                make(1L,"b",0,1,"cool"),
                make(1L,"c",0,1,"cool"),
                make(1L,"d",0,1,"cool"),
                make(1L,"e",0,1,"cool")
        );
        List<MyType> revised = Arrays.asList(
                make(1L,"a",0,1,"cool"),
                make(1L,"b",0,1,"cool"),
                make(1L,"c",0,1,"cool"),
                make(1L,"d",1,1,"cool"),  // region changed
                make(1L,"e",0,2,"coool")  // flavour & bio changed
        );
        ComparisonResultStats stats = new ComparisonResultStats();

        // act
        differencer.computeDiff(original, revised, stats);

        // assert
        assertThat(stats.getEqualCount().get(), equalTo(3));
        assertThat(stats.getChangedCount().get(), equalTo(2));
        assertThat(stats.getAddedCount().get(), equalTo(0));
        assertThat(stats.getDroppedCount().get(), equalTo(0));
        assertTrue(stats.getChangedKeys().contains(key("d", 1L)));
        assertTrue(stats.getChangedKeys().contains(key("e", 1L)));
    }

    @Test
    public void testAdditionAtStart() {
        // arrange
        List<MyType> original = Arrays.asList(
                make(1L,"b",0,1,"cool"),
                make(1L,"c",0,1,"cool"),
                make(1L,"d",0,1,"cool"),
                make(1L,"e",0,1,"cool"),
                make(1L,"f",0,1,"cool")
        );
        List<MyType> revised = Arrays.asList(
                make(1L,"a",0,1,"cool"),
                make(1L,"b",0,1,"cool"),
                make(1L,"c",0,1,"cool"),
                make(1L,"d",0,1,"cool"),
                make(1L,"e",0,1,"cool"),
                make(1L,"f",0,1,"cool")
        );
        ComparisonResultStats stats = new ComparisonResultStats();

        // act
        differencer.computeDiff(original, revised, stats);

        // assert
        assertThat(stats.getEqualCount().get(), equalTo(5));
        assertThat(stats.getChangedCount().get(), equalTo(0));
        assertThat(stats.getAddedCount().get(), equalTo(1));
        assertThat(stats.getDroppedCount().get(), equalTo(0));
        assertTrue(stats.getAddedKeys().contains(key("a", 1L)));
    }

    @Test
    public void testAdditionAtEnd() {
        // arrange
        List<MyType> original = Arrays.asList(
                make(1L,"a",0,1,"cool"),
                make(1L,"b",0,1,"cool"),
                make(1L,"c",0,1,"cool"),
                make(1L,"d",0,1,"cool"),
                make(1L,"e",0,1,"cool")
        );
        List<MyType> revised = Arrays.asList(
                make(1L,"a",0,1,"cool"),
                make(1L,"b",0,1,"cool"),
                make(1L,"c",0,1,"cool"),
                make(1L,"d",0,1,"cool"),
                make(1L,"e",0,1,"cool"),
                make(1L,"f",0,1,"cool")
        );
        ComparisonResultStats stats = new ComparisonResultStats();

        // act
        differencer.computeDiff(original, revised, stats);

        // assert
        assertThat(stats.getEqualCount().get(), equalTo(5));
        assertThat(stats.getChangedCount().get(), equalTo(0));
        assertThat(stats.getAddedCount().get(), equalTo(1));
        assertThat(stats.getDroppedCount().get(), equalTo(0));
        assertTrue(stats.getAddedKeys().contains(key("f", 1L)));
    }

    @Test
    public void testAdditionInMiddle() {
        // arrange
        List<MyType> original = Arrays.asList(
                make(1L,"a",0,1,"cool"),
                make(1L,"b",0,1,"cool"),
                make(1L,"c",0,1,"cool"),
                make(1L,"e",0,1,"cool")
        );
        List<MyType> revised = Arrays.asList(
                make(1L,"a",0,1,"cool"),
                make(1L,"b",0,1,"cool"),
                make(1L,"c",0,1,"cool"),
                make(1L,"d",0,1,"cool"),
                make(1L,"e",0,1,"cool")
        );
        ComparisonResultStats stats = new ComparisonResultStats();

        // act
        differencer.computeDiff(original, revised, stats);

        // assert
        assertThat(stats.getEqualCount().get(), equalTo(4));
        assertThat(stats.getChangedCount().get(), equalTo(0));
        assertThat(stats.getAddedCount().get(), equalTo(1));
        assertThat(stats.getDroppedCount().get(), equalTo(0));
        assertTrue(stats.getAddedKeys().contains(key("d", 1L)));
    }

    @Test
    public void testDropAtStart() {
        // arrange
        List<MyType> original = Arrays.asList(
                make(1L,"a",0,1,"cool"),
                make(1L,"b",0,1,"cool"),
                make(1L,"c",0,1,"cool"),
                make(1L,"d",0,1,"cool"),
                make(1L,"e",0,1,"cool"),
                make(1L,"f",0,1,"cool")
        );
        List<MyType> revised = Arrays.asList(
                make(1L,"b",0,1,"cool"),
                make(1L,"c",0,1,"cool"),
                make(1L,"d",0,1,"cool"),
                make(1L,"e",0,1,"cool"),
                make(1L,"f",0,1,"cool")
        );
        ComparisonResultStats stats = new ComparisonResultStats();

        // act
        differencer.computeDiff(original, revised, stats);

        // assert
        assertThat(stats.getEqualCount().get(), equalTo(5));
        assertThat(stats.getChangedCount().get(), equalTo(0));
        assertThat(stats.getAddedCount().get(), equalTo(0));
        assertThat(stats.getDroppedCount().get(), equalTo(1));
        assertTrue(stats.getDroppedKeys().contains(key("a", 1L)));
    }

    @Test
    public void testDropAtEnd() {
        // arrange
        List<MyType> original = Arrays.asList(
                make(1L,"a",0,1,"cool"),
                make(1L,"b",0,1,"cool"),
                make(1L,"c",0,1,"cool"),
                make(1L,"d",0,1,"cool"),
                make(1L,"e",0,1,"cool"),
                make(1L,"f",0,1,"cool")
        );
        List<MyType> revised = Arrays.asList(
                make(1L,"a",0,1,"cool"),
                make(1L,"b",0,1,"cool"),
                make(1L,"c",0,1,"cool"),
                make(1L,"d",0,1,"cool"),
                make(1L,"e",0,1,"cool")
        );
        ComparisonResultStats stats = new ComparisonResultStats();

        // act
        differencer.computeDiff(original, revised, stats);

        // assert
        assertThat(stats.getEqualCount().get(), equalTo(5));
        assertThat(stats.getChangedCount().get(), equalTo(0));
        assertThat(stats.getAddedCount().get(), equalTo(0));
        assertThat(stats.getDroppedCount().get(), equalTo(1));
        assertTrue(stats.getDroppedKeys().contains(key("f", 1L)));
    }

    @Test
    public void testDropInMiddle() {
        // arrange
        List<MyType> original = Arrays.asList(
                make(1L,"a",0,1,"cool"),
                make(1L,"b",0,1,"cool"),
                make(1L,"c",0,1,"cool"),
                make(1L,"d",0,1,"cool"),
                make(1L,"e",0,1,"cool"),
                make(1L,"f",0,1,"cool")
        );
        List<MyType> revised = Arrays.asList(
                make(1L,"a",0,1,"cool"),
                make(1L,"b",0,1,"cool"),
                make(1L,"d",0,1,"cool"),
                make(1L,"e",0,1,"cool"),
                make(1L,"f",0,1,"cool")
        );
        ComparisonResultStats stats = new ComparisonResultStats();

        // act
        differencer.computeDiff(original, revised, stats);

        // assert
        assertThat(stats.getEqualCount().get(), equalTo(5));
        assertThat(stats.getChangedCount().get(), equalTo(0));
        assertThat(stats.getAddedCount().get(), equalTo(0));
        assertThat(stats.getDroppedCount().get(), equalTo(1));
        assertTrue(stats.getDroppedKeys().contains(key("c", 1L)));
    }

    @Test
    public void staticFactoryWithValidation() {
        RegularDifferencer v = RegularDifferencer.withValidation(keyComparator, dataComparator);
        assertTrue(v.isValidationOn());
    }

    @Test
    public void staticFactoryWithoutValidation() {
        RegularDifferencer v = RegularDifferencer.withoutValidation(keyComparator, dataComparator);
        assertTrue(!v.isValidationOn());
    }

    private MyType make(long domain, String name, int region, int flavour, String bio) {
        return MyType.builder()
                .domain(domain)
                .name(name)
                .region(region)
                .flavour(flavour)
                .bio(bio)
                .build();
    }

    static MyTypeKey key(String name, long domain) {
        return new MyTypeKey(name, domain);
    }

}