package sandkev.differencer;

import sandkev.differencer.api.ComparisonResult;

import java.util.*;

/**
 * Container for collection of differences.
 */
public class DiffSummary {
    private final Map<ComparisonResult, Map<String, List<Diff>>> diffsByType;
    public DiffSummary() {
        this.diffsByType = new HashMap<>();
    }
    public void addDiff( String fieldName, Object expectedValue, Object rejectedValue, ComparisonResult diffType) {
        Map<String, List<Diff>> diffsForType = diffsByType.computeIfAbsent(diffType, k -> new HashMap<>());
        List<Diff> diffsForField = diffsForType.computeIfAbsent(fieldName, k -> new ArrayList<>());
        diffsForField.add(Diff.builder().expectedValue(expectedValue).rejectedValue(rejectedValue).build());
    }

    public ComparisonResult getComparisonResult() {
        if( diffsByType.isEmpty()) {
            return ComparisonResult.Equal;
        } else if (diffsByType.size() == 1) {
            return diffsByType.keySet().iterator().next();
        } else {
            return ComparisonResult.Changed;
        }
    }

    @Override
    public String toString() {
        return diffsByType.toString();
    }
}
