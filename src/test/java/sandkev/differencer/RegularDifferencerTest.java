package sandkev.differencer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import sandkev.differencer.api.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class RegularDifferencerTest {

    private static final Comparator<MyType> KEY_COMPARATOR =
            Comparator.comparing(MyType::getDomain)
                    .thenComparing(MyType::getName);

    private static final DiffComparator<MyType> DATA_COMPARATOR = (o1, o2) -> {
        BigDecimal tolerance = new BigDecimal("0.0001");
        DiffSummary diffs = new DiffSummary();
        if (!o1.getSensitiveValue().equals(o2.getSensitiveValue())) {
            BigDecimal absDiff = o1.getSensitiveValue().abs().subtract(o2.getSensitiveValue().abs());
            if( absDiff.compareTo(tolerance) <= 0) {
                diffs.addDiff("sensitiveValue", o1.getSensitiveValue(), o2.getSensitiveValue(), ComparisonResult.ApproximatelyEqual);
            } else {
                diffs.addDiff("sensitiveValue", o1.getSensitiveValue(), o2.getSensitiveValue(), ComparisonResult.Changed);
            }
        }
        if (o1.getRegion() != o2.getRegion()) {
            diffs.addDiff("region", o1.getRegion(), o2.getRegion(), ComparisonResult.Changed);
        }
        if (!Objects.equals(o1.getBio(), o2.getBio())) {
            diffs.addDiff("bio", o1.getBio(), o2.getBio(), ComparisonResult.Changed);
        }
        return diffs;
    };

    private DiffAlgorithm<MyType,MyTypeKey> differencer;
    private ComparisonResultStats<MyType,MyTypeKey> stats;

    @BeforeEach
    void setUp() {
        differencer = new RegularDifferencer<>(KEY_COMPARATOR, DATA_COMPARATOR);
        stats       = new ComparisonResultStats<>();
    }

    static record MyTypeKey(String name, Long domain) {}

    static class MyType implements Identifiable<MyTypeKey> {
        private final String name;
        private final Long   domain;
        private final int    region;
        private final BigDecimal sensitiveValue;
        private final String bio;
        private final LocalDateTime lastUpdated;

        MyType(String name, Long domain, int region, BigDecimal sensitiveValue, String bio, LocalDateTime lastUpdated) {
            this.name        = name;
            this.domain      = domain;
            this.region      = region;
            this.sensitiveValue = sensitiveValue;
            this.bio         = bio;
            this.lastUpdated = lastUpdated;
        }

        @Override
        public MyTypeKey getId() {
            return new MyTypeKey(name, domain);
        }
        public String getName()       { return name; }
        public Long   getDomain()     { return domain; }
        public int    getRegion()     { return region; }
        public BigDecimal getSensitiveValue()    { return sensitiveValue; }
        public String getBio()        { return bio; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
    }

    private static MyType make(long domain, String name, int region, BigDecimal sensitiveValue, String bio) {
        return new MyType(name, domain, region, sensitiveValue, bio, LocalDateTime.now());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("diffScenarios")
    void computeDiff_variousScenarios(String description,
                                      List<MyType> original,
                                      List<MyType> revised,
                                      int expectEquals,
                                      int expectApproximatelyEquals,
                                      int expectAdded,
                                      int expectDropped,
                                      int expectChanged,
                                      Set<MyTypeKey> expectAddedKeys,
                                      Set<MyTypeKey> expectDroppedKeys,
                                      Set<MyTypeKey> expectChangedKeys) {
        // act
        differencer.computeDiff(original, revised, stats);

        // assert counts
        assertAll(description,
                () -> assertEquals(expectEquals,  stats.getEqualCount().get(),   "equals"),
                () -> assertEquals(expectApproximatelyEquals,  stats.getApproximatelyEqualCount().get(),   "approximatelyEquals"),
                () -> assertEquals(expectAdded,   stats.getAddedCount().get(),   "added"),
                () -> assertEquals(expectDropped, stats.getDroppedCount().get(), "dropped"),
                () -> assertEquals(expectChanged, stats.getChangedCount().get(), "changed")
        );

        // assert specific keys
        assertEquals(expectAddedKeys,   stats.getAddedKeys(),   "added keys");
        assertEquals(expectDroppedKeys, stats.getDroppedKeys(), "dropped keys");
        assertEquals(expectChangedKeys, stats.getChangedKeys(), "changed keys");
    }

    static Stream<Arguments> diffScenarios() {
        return Stream.of(
                Arguments.of("all equal",
                        List.of(make(1,"a",0,BigDecimal.valueOf(1.0),"x"),
                                make(1,"b",0,BigDecimal.valueOf(1.0),"y"),
                                make(1,"c",0,BigDecimal.valueOf(1.0),"z")),
                        List.of(make(1,"a",0,BigDecimal.valueOf(1.0),"x"),
                                make(1,"b",0,BigDecimal.valueOf(1.0),"y"),
                                make(1,"c",0,BigDecimal.valueOf(1.0),"z")),
                        3, 0, 0, 0, 0,
                        Set.of(), Set.of(), Set.of()
                ),
                Arguments.of("one approx equal",
                        List.of(make(1,"a",0,BigDecimal.valueOf(1.000001),"x"),
                                make(1,"b",0,BigDecimal.valueOf(1.0),"y"),
                                make(1,"c",0,BigDecimal.valueOf(1.0),"z")),
                        List.of(make(1,"a",0,BigDecimal.valueOf(1.0),"x"),
                                make(1,"b",0,BigDecimal.valueOf(1.0),"y"),
                                make(1,"c",0,BigDecimal.valueOf(1.0),"z")),
                        2, 1, 0, 0, 0,
                        Set.of(), Set.of(), Set.of()
                ),
                Arguments.of("two changes, no drop or add",
                        List.of(make(1L,"a",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"b",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"c",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"d",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"e",0,BigDecimal.valueOf(1.0),"cool")),
                        List.of(make(1L,"a",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"b",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"c",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"d",1,BigDecimal.valueOf(1.0),"cool"),   // region changed
                                make(1L,"e",0,BigDecimal.valueOf(2.0),"coool")), // flavour & bio changed
                        3, 0, 0, 0, 2,
                        Set.of(), Set.of(), Set.of(new MyTypeKey("d", 1L),new MyTypeKey("e", 1L))
                ),
                Arguments.of("one addition at start",
                        List.of(make(1L,"b",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"c",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"d",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"e",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"f",0,BigDecimal.valueOf(1.0),"cool")),
                        List.of(make(1L,"a",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"b",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"c",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"d",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"e",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"f",0,BigDecimal.valueOf(1.0),"cool")),
                        5, 0, 1, 0, 0,
                        Set.of(new MyTypeKey("a", 1L)),
                        Set.of(),
                        Set.of()
                ),
                Arguments.of("one addition at end",
                        List.of(make(1L,"a",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"b",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"c",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"d",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"e",0,BigDecimal.valueOf(1.0),"cool")),
                        List.of(make(1L,"a",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"b",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"c",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"d",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"e",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"f",0,BigDecimal.valueOf(1.0),"cool")),
                        5, 0, 1, 0, 0,
                        Set.of(new MyTypeKey("f", 1L)),
                        Set.of(),
                        Set.of()
                ),
                Arguments.of("one addition in middle",
                        List.of(make(1L,"a",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"b",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"c",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"e",0,BigDecimal.valueOf(1.0),"cool")),
                        List.of(make(1L,"a",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"b",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"c",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"d",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"e",0,BigDecimal.valueOf(1.0),"cool")),
                        4, 0, 1, 0, 0,
                        Set.of(new MyTypeKey("d", 1L)),
                        Set.of(),
                        Set.of()
                ),
                Arguments.of("one drop at start",
                        List.of(make(1L,"a",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"b",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"c",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"d",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"e",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"f",0,BigDecimal.valueOf(1.0),"cool")),
                        List.of(make(1L,"b",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"c",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"d",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"e",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"f",0,BigDecimal.valueOf(1.0),"cool")),
                        5, 0, 0, 1, 0,
                        Set.of(),
                        Set.of(new MyTypeKey("a", 1L)),
                        Set.of()
                ),
                Arguments.of("one drop in middle",
                        List.of(make(1L,"a",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"b",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"c",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"d",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"e",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"f",0,BigDecimal.valueOf(1.0),"cool")),
                        List.of(make(1L,"a",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"b",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"d",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"e",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"f",0,BigDecimal.valueOf(1.0),"cool")),
                        5, 0, 0, 1, 0,
                        Set.of(),
                        Set.of(new MyTypeKey("c", 1L)),
                        Set.of()
                ),
                Arguments.of("one drop at end",
                        List.of(make(1L,"a",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"b",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"c",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"d",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"e",0,BigDecimal.valueOf(1.0),"cool")),
                        List.of(make(1L,"a",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"b",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"c",0,BigDecimal.valueOf(1.0),"cool"),
                                make(1L,"d",0,BigDecimal.valueOf(1.0),"cool")),
                        4, 0, 0, 1, 0,
                        Set.of(),
                        Set.of(new MyTypeKey("e", 1L)),
                        Set.of()
                ),
                Arguments.of("one change",
                        List.of(make(1,"a",0,BigDecimal.valueOf(1.0),"x"),
                                make(1,"b",0,BigDecimal.valueOf(1.0),"y"),
                                make(1,"c",0,BigDecimal.valueOf(1.0),"z")),
                        List.of(make(1,"a",0,BigDecimal.valueOf(1.0),"x"),
                                make(1,"b",1,BigDecimal.valueOf(1.0),"y"),
                                make(1,"c",0,BigDecimal.valueOf(1.0),"z")),
                        2, 0, 0, 0, 1,
                        Set.of(),
                        Set.of(),
                        Set.of(new MyTypeKey("b", 1L))
                ),
                Arguments.of("mixed (drop, change, add)",
                        List.of(make(1,"a",0,BigDecimal.valueOf(1.0),"x"),
                                make(1,"b",0,BigDecimal.valueOf(1.0),"y"),
                                make(1,"c",0,BigDecimal.valueOf(1.0),"z")),
                        List.of(make(1,"b",1,BigDecimal.valueOf(1.0),"y"),
                                make(1,"c",0,BigDecimal.valueOf(1.0),"z"),
                                make(1,"d",0,BigDecimal.valueOf(1.0),"w")),
                        1, 0, 1, 1, 1,
                        Set.of(new MyTypeKey("d", 1L)),
                        Set.of(new MyTypeKey("a", 1L)),
                        Set.of(new MyTypeKey("b", 1L))
                )
        );
    }

    @Test
    void throwsOnUnsortedInputWhenValidateOn() {
        var unsorted = List.of(
                make(1,"b",0,BigDecimal.valueOf(1.0),"cool"),
                make(1,"a",0,BigDecimal.valueOf(1.0),"cool")
        );
        var vDiff = RegularDifferencer.withValidation(KEY_COMPARATOR, DATA_COMPARATOR);
        assertThrows(IllegalArgumentException.class,
                () -> vDiff.computeDiff(unsorted, unsorted, new ComparisonResultStats<>())
        );
    }

    @Test
    void staticFactoryMethods_toggleValidationFlag() {
        var withVal    = RegularDifferencer.withValidation(KEY_COMPARATOR, DATA_COMPARATOR);
        var withoutVal = RegularDifferencer.withoutValidation(KEY_COMPARATOR, DATA_COMPARATOR);

        assertTrue(withVal.isValidationOn(),   "withValidation should enable validation");
        assertFalse(withoutVal.isValidationOn(), "withoutValidation should disable validation");
    }

    @Test
    void diffAndCollect_detectsChangeAndAddition() {
        var expected = List.of(
                make(1,"b",0,BigDecimal.valueOf(1.0),"cool"),
                make(1,"a",0,BigDecimal.valueOf(1.0),"cool")
        );
        var actual = List.of(
                make(1,"b",0,BigDecimal.valueOf(1.0),"cool"),
                make(1,"a",0,BigDecimal.valueOf(1.0),"un-cool"),
                make(1,"x",0,BigDecimal.valueOf(1.0),"cool")
        );

        ComparisonResultStats<String, String> stats = differencer.diffAndCollect(expected, actual);

        assertEquals(1, stats.getEqualCount().get());
        assertEquals(1, stats.getChangedCount().get());
        assertTrue(stats.getChangedKeys().contains(new MyTypeKey("a", 1L)));

        assertEquals(1, stats.getAddedCount().get());
        assertTrue(stats.getAddedKeys().contains(new MyTypeKey("x", 1L)));

        assertEquals(0, stats.getDroppedCount().get());
    }
}
