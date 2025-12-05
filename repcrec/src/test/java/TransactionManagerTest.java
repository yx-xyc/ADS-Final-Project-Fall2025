import com.ads.DataManager;
import com.ads.TransactionManager;
import com.ads.interfaces.IDataManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for TransactionManager.
 * Tests basic transaction operations and SSI validation.
 */
public class TransactionManagerTest {
    private TransactionManager tm;
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    public void setUp() {
        // Create DataManagers for all 10 sites
        Map<Integer, IDataManager> dataManagers = new HashMap<>();
        for (int i = 1; i <= 10; i++) {
            dataManagers.put(i, new DataManager(i));
        }

        tm = new TransactionManager(dataManagers);

        // Capture System.out for assertion
        System.setOut(new PrintStream(outputStream));
    }

    @Test
    public void testBasicBeginAndWrite() {
        tm.begin("T1");
        tm.write("T1", "x1", 100);

        String output = outputStream.toString();
        assertTrue(output.contains("Transaction T1 begins"));
        assertTrue(output.contains("Transaction T1 writes 100 to x1"));
    }

    @Test
    public void testReadOwnWrite() {
        tm.begin("T1");
        tm.write("T1", "x1", 100);
        tm.read("T1", "x1");

        String output = outputStream.toString();
        assertTrue(output.contains("T1 reads x1: 100 (own write)"));
    }

    @Test
    public void testBasicRead() {
        tm.begin("T1");
        tm.read("T1", "x2"); // x2 is replicated, initial value = 20

        String output = outputStream.toString();
        assertTrue(output.contains("T1 reads x2: 20"));
    }

    @Test
    public void testReadOnlyTransactionCommits() {
        tm.begin("T1");
        tm.read("T1", "x2");
        tm.end("T1");

        String output = outputStream.toString();
        assertTrue(output.contains("Transaction T1 commits (read-only)"));
    }

    @Test
    public void testWriteTransactionCommits() {
        tm.begin("T1");
        tm.write("T1", "x1", 100);
        tm.end("T1");

        String output = outputStream.toString();
        assertTrue(output.contains("Transaction T1 commits"));
        assertFalse(output.contains("aborts"));
    }

    @Test
    public void testFirstCommitterWins() {
        // Test 1 from specification
        tm.begin("T1");
        tm.begin("T2");
        tm.write("T1", "x1", 101);
        tm.write("T2", "x2", 202);
        tm.write("T1", "x2", 102);
        tm.write("T2", "x1", 201);
        tm.end("T2"); // T2 commits first
        tm.end("T1"); // T1 should abort (first-committer-wins)

        String output = outputStream.toString();
        assertTrue(output.contains("Transaction T2 commits"));
        assertTrue(output.contains("Transaction T1 aborts: Write-write conflict"));
    }

    @Test
    public void testSnapshotIsolation() {
        // Test 2 from specification
        tm.begin("T1");
        tm.begin("T2");
        tm.write("T1", "x1", 101);
        tm.read("T2", "x2"); // Should read initial value (20)
        tm.write("T1", "x2", 102);
        tm.read("T2", "x1"); // Should read initial value (10)
        tm.end("T1");
        tm.end("T2");

        String output = outputStream.toString();
        // T2 should read initial values (snapshot at T2's start time)
        assertTrue(output.contains("T2 reads x2: 20"));
        assertTrue(output.contains("T2 reads x1: 10"));
        assertTrue(output.contains("Transaction T1 commits"));
        assertTrue(output.contains("Transaction T2 commits"));
    }

    @Test
    public void testSiteFailure() {
        tm.begin("T1");
        tm.read("T1", "x2"); // Read from a site
        tm.fail(1); // Fail site 1 (if T1 accessed it)

        String output = outputStream.toString();
        assertTrue(output.contains("Site 1 fails"));
        // T1 may abort if it accessed site 1
    }

    @Test
    public void testSiteRecovery() {
        tm.fail(1);
        tm.recover(1);

        String output = outputStream.toString();
        assertTrue(output.contains("Site 1 fails"));
        assertTrue(output.contains("Site 1 recovers"));
    }

    @Test
    public void testDump() {
        tm.dump();

        String output = outputStream.toString();
        assertTrue(output.contains("=== Database Dump ==="));
        assertTrue(output.contains("x1"));
        assertTrue(output.contains("x20"));
    }

    @Test
    public void testTransactionAbortOnInvalidState() {
        tm.begin("T1");
        tm.end("T1"); // T1 commits (read-only)

        // Try to write to T1 after commit
        outputStream.reset();
        tm.write("T1", "x1", 100);

        String output = outputStream.toString();
        assertTrue(output.contains("Transaction T1 is not active"));
    }

    @Test
    public void testMultipleTransactionsConcurrent() {
        tm.begin("T1");
        tm.begin("T2");
        tm.begin("T3");

        tm.write("T1", "x1", 100);
        tm.write("T2", "x2", 200);
        tm.write("T3", "x3", 300);

        tm.end("T1");
        tm.end("T2");
        tm.end("T3");

        String output = outputStream.toString();
        assertTrue(output.contains("Transaction T1 commits"));
        assertTrue(output.contains("Transaction T2 commits"));
        assertTrue(output.contains("Transaction T3 commits"));
    }

    // Cleanup after tests
    @org.junit.jupiter.api.AfterEach
    public void tearDown() {
        System.setOut(originalOut);
    }
}
