import com.ads.TransactionManager;
import com.ads.interfaces.IDataManager;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple test to verify basic functionality.
 */
public class SimpleTest {
    public static void main(String[] args) {
        System.out.println("=== Simple TransactionManager Test ===\n");

        // Create stub DataManagers
        Map<Integer, IDataManager> dataManagers = new HashMap<>();
        for (int i = 1; i <= 10; i++) {
            dataManagers.put(i, new StubDataManager(i));
        }

        TransactionManager tm = new TransactionManager(dataManagers);

        System.out.println("Test 1: Basic Read/Write");
        tm.begin("T1");
        tm.read("T1", "x2");  // Should read 20
        tm.write("T1", "x1", 100);
        tm.read("T1", "x1");  // Should read 100 (own write)
        tm.end("T1");
        System.out.println();

        System.out.println("Test 2: First-Committer-Wins (from spec Test 1)");
        tm.begin("T1");
        tm.begin("T2");
        tm.write("T1", "x1", 101);
        tm.write("T2", "x2", 202);
        tm.write("T1", "x2", 102);
        tm.write("T2", "x1", 201);
        tm.end("T2");  // T2 commits first
        tm.end("T1");  // T1 should abort
        System.out.println();

        System.out.println("Test 3: Snapshot Isolation (from spec Test 2)");
        tm.begin("T3");
        tm.begin("T4");
        tm.write("T3", "x1", 101);
        tm.read("T4", "x2");  // Should read initial value
        tm.write("T3", "x2", 102);
        tm.read("T4", "x1");  // Should read initial value
        tm.end("T3");
        tm.end("T4");
        System.out.println();

        System.out.println("Test 4: Dump");
        tm.dump();
    }
}
