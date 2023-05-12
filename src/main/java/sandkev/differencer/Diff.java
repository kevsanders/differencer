package sandkev.differencer;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class Diff {
    Object expectedValue;
    Object rejectedValue;

    @Override
    public String toString() {
        return "expectedValue=" + expectedValue +
                ", rejectedValue=" + rejectedValue;
    }
}
