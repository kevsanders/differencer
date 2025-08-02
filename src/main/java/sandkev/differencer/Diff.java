package sandkev.differencer;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class Diff {
    Object expectedValue;
    Object actualValue;

    @Override
    public String toString() {
        return "expectedValue=" + expectedValue +
                ", actualValue=" + actualValue;
    }
}
