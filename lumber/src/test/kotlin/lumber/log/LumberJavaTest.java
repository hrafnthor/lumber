package lumber.log;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

public class LumberJavaTest {
    @SuppressWarnings("ConstantConditions")
    @Test
    public void nullTree() {
        try {
            Lumber.plant((Lumber.Tree) null);
            fail();
        } catch (NullPointerException ignored) {
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Test public void nullTreeArray() {
        try {
            Lumber.plant((Lumber.Tree[]) null);
            fail();
        } catch (NullPointerException ignored) {
        }
        try {
            Lumber.plant(new Lumber.Tree[] { null });
            fail();
        } catch (IllegalArgumentException ignored) {
        }
    }
}
