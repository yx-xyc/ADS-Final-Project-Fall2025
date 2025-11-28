import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ads.Variable;
import com.ads.VersionedValue;

public class VariableTests {

    @Test
    void testGetLatestVersionReturnsNullWhenEmpty() {
        final Variable v = new Variable("x1", true);
        assertNull(v.getLatestVersion(), "Expected latest version to be null for empty commit log");
    }

    @Test
    void testAddCommittedVersionAndGetLatest() {
        final Variable v = new Variable("x1", true);
        final VersionedValue ver1 = new VersionedValue(10, 1);
        v.addCommittedVersion(ver1);
        assertSame(ver1, v.getLatestVersion(), "Latest version should be the one just added");
    }

    @Test
    void testGetVersionForWithMultipleVersions() {
        final Variable v = new Variable("x2", false);
        v.addCommittedVersion(new VersionedValue(20, 2));
        v.addCommittedVersion(new VersionedValue(25, 5));
        v.addCommittedVersion(new VersionedValue(30, 10));

        final VersionedValue result = v.getVersionFor(7); // should pick commitTime 5
        assertNotNull(result, "Expected a version for start time 7");
        assertEquals(25, result.getValue());
        assertEquals(5, result.getCommitTime());
    }

    @Test
    void testGetVersionForBeforeAnyVersionReturnsNull() {
        final Variable v = new Variable("x3", false);
        v.addCommittedVersion(new VersionedValue(100, 100));
        assertNull(v.getVersionFor(50), "Start time before any commit should return null");
    }
}
