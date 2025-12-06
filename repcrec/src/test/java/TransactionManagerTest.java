import com.ads.Command;
import com.ads.DataManager;
import com.ads.TransactionManager;
import com.ads.CommandType;
import com.ads.interfaces.IDataManager;
import com.ads.interfaces.ITransactionManager;
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

//we change every invokation of TM methods to use execute(new Command(...)) instead of direct method calls.
public class TransactionManagerTest {
    private ITransactionManager tm;
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
        tm.execute(new Command(CommandType.BEGIN, new String[]{"T1"}));
        tm.execute(new Command(CommandType.WRITE, new String[]{"T1", "x1", "100"}));

        String output = outputStream.toString();
        assertTrue(output.contains("Transaction T1 begins"));
        assertTrue(output.contains("Transaction T1 writes 100 to x1"));
    }

    @Test
    public void testReadOwnWrite() {
        tm.execute(new Command(CommandType.BEGIN, new String[]{"T1"}));
        tm.execute(new Command(CommandType.WRITE, new String[]{"T1", "x1", "100"}));
        tm.execute(new Command(CommandType.READ, new String[]{"T1", "x1"}));

        String output = outputStream.toString();
        System.out.println("Output Stream: " + output); // Debug print
        assertTrue(output.contains("x1: 100"));
    }

    @Test
    public void testBasicRead() {
        tm.execute(new Command(CommandType.BEGIN, new String[]{"T1"}));
        tm.execute(new Command(CommandType.READ, new String[]{"T1", "x2"})); // x2 is replicated, initial value = 20

        String output = outputStream.toString();
        assertTrue(output.contains("x2: 20"));
    }

    @Test
    public void testReadOnlyTransactionCommits() {
        tm.execute(new Command(CommandType.BEGIN, new String[]{"T1"}));
        tm.execute(new Command(CommandType.READ, new String[]{"T1", "x2"}));
        tm.execute(new Command(CommandType.END, new String[]{"T1"}));

        String output = outputStream.toString();
        assertTrue(output.contains("T1 commits"));
    }

    @Test
    public void testWriteTransactionCommits() {
        tm.execute(new Command(CommandType.BEGIN, new String[]{"T1"}));
        tm.execute(new Command(CommandType.WRITE, new String[]{"T1", "x1", "100"}));
        tm.execute(new Command(CommandType.END, new String[]{"T1"}));

        String output = outputStream.toString();
        assertTrue(output.contains("T1 commits"));
        assertFalse(output.contains("aborts"));
    }

    @Test
    public void testFirstCommitterWins() {
        // Test 1 from specification
        tm.execute(new Command(CommandType.BEGIN, new String[]{"T1"}));
        tm.execute(new Command(CommandType.BEGIN, new String[]{"T2"}));
        tm.execute(new Command(CommandType.WRITE, new String[]{"T1", "x1", "101"}));
        tm.execute(new Command(CommandType.WRITE, new String[]{"T2", "x2", "202"}));
        tm.execute(new Command(CommandType.WRITE, new String[]{"T1", "x2", "102"}));
        tm.execute(new Command(CommandType.WRITE, new String[]{"T2", "x1", "201"}));
        tm.execute(new Command(CommandType.END, new String[]{"T2"})); // T2 commits first
        tm.execute(new Command(CommandType.END, new String[]{"T1"})); // T1 should abort (first-committer-wins)

        String output = outputStream.toString();
        assertTrue(output.contains("T2 commits"));
        assertTrue(output.contains("T1 aborts"));
    }

    @Test
    public void testSnapshotIsolation() {
        // Test 2 from specification
        tm.execute(new Command(CommandType.BEGIN, new String[]{"T1"}));
        tm.execute(new Command(CommandType.BEGIN, new String[]{"T2"}));
        tm.execute(new Command(CommandType.WRITE, new String[]{"T1", "x1", "101"}));
        tm.execute(new Command(CommandType.READ, new String[]{"T2", "x2"})); // Should read initial value (20)
        tm.execute(new Command(CommandType.WRITE, new String[]{"T1", "x2", "102"}));
        tm.execute(new Command(CommandType.READ, new String[]{"T2", "x1"})); // Should read initial value (10)
        tm.execute(new Command(CommandType.END, new String[]{"T1"}));
        tm.execute(new Command(CommandType.END, new String[]{"T2"}));

        String output = outputStream.toString();
        // T2 should read initial values (snapshot at T2's start time)
        assertTrue(output.contains("x2: 20"));
        assertTrue(output.contains("x1: 10"));
        assertTrue(output.contains("T1 commits"));
        assertTrue(output.contains("T2 commits"));
    }

    @Test
    public void testSiteFailure() {
        tm.execute(new Command(CommandType.BEGIN, new String[]{"T1"}));
        tm.execute(new Command(CommandType.READ, new String[]{"T1", "x2"})); // Read from a site
        tm.execute(new Command(CommandType.FAIL, new String[]{"1"})); // Fail site 1 (if T1 accessed it)

        String output = outputStream.toString();
        assertTrue(output.contains("Site 1 fails"));
        // T1 may abort if it accessed site 1
    }

    @Test
    public void testSiteRecovery() {
        tm.execute(new Command(CommandType.FAIL, new String[]{"1"}));
        tm.execute(new Command(CommandType.RECOVER, new String[]{"1"}));
        
        String output = outputStream.toString();
        assertTrue(output.contains("Site 1 fails"));
        assertTrue(output.contains("Site 1 recovers"));
    }

    @Test
    public void testDump() {
        tm.execute(new Command(CommandType.DUMP, new String[]{}));

        String output = outputStream.toString();
        assertTrue(output.contains("=== Database Dump ==="));
        assertTrue(output.contains("x1"));
        assertTrue(output.contains("x20"));
    }

    @Test
    public void testTransactionAbortOnInvalidState() {
        tm.execute(new Command(CommandType.BEGIN, new String[]{"T1"}));
        tm.execute(new Command(CommandType.END, new String[]{"T1"})); // T1 commits (read-only)

        // Try to write to T1 after commit
        outputStream.reset();
        tm.execute(new Command(CommandType.WRITE, new String[]{"T1", "x1", "100"}));

        String output = outputStream.toString();
        assertTrue(output.contains("Transaction T1 is not active"));
    }

    @Test
    public void testMultipleTransactionsConcurrent() {
        tm.execute(new Command(CommandType.BEGIN, new String[]{"T1"}));
        tm.execute(new Command(CommandType.BEGIN, new String[]{"T2"}));
        tm.execute(new Command(CommandType.BEGIN, new String[]{"T3"}));

        tm.execute(new Command(CommandType.WRITE, new String[]{"T1", "x1", "100"}));
        tm.execute(new Command(CommandType.WRITE, new String[]{"T2", "x2", "200"}));
        tm.execute(new Command(CommandType.WRITE, new String[]{"T3", "x3", "300"}));

        tm.execute(new Command(CommandType.END, new String[]{"T1"}));
        tm.execute(new Command(CommandType.END, new String[]{"T2"}));
        tm.execute(new Command(CommandType.END, new String[]{"T3"}));

        String output = outputStream.toString();
        assertTrue(output.contains("T1 commits"));
        assertTrue(output.contains("T2 commits"));
        assertTrue(output.contains("T3 commits"));
    }

    // Cleanup after tests
    @org.junit.jupiter.api.AfterEach
    public void tearDown() {
        System.setOut(originalOut);
    }
}
