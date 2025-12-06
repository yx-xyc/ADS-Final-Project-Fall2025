import com.ads.CommandType;
import com.ads.Command;
import com.ads.DataManager;
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

        // Create DataManagers
        Map<Integer, IDataManager> dataManagers = new HashMap<>();
        for (int i = 1; i <= 10; i++) {
            dataManagers.put(i, new DataManager(i));
        }

        TransactionManager tm = new TransactionManager(dataManagers);

        System.out.println("Test 1: Basic Read/Write");
        tm.execute(new Command(CommandType.BEGIN, new String[]{"T1"}));
        tm.execute(new Command(CommandType.READ, new String[]{"T1", "x2"}));  // Should read 20
        tm.execute(new Command(CommandType.WRITE, new String[]{"T1", "x1", "100"}));
        tm.execute(new Command(CommandType.READ, new String[]{"T1", "x1"}));  // Should read 100 (own write)
        tm.execute(new Command(CommandType.END, new String[]{"T1"}));
        System.out.println();

        System.out.println("Test 2: First-Committer-Wins (from spec Test 1)");
        tm.execute(new Command(CommandType.BEGIN, new String[]{"T1"}));
        tm.execute(new Command(CommandType.BEGIN, new String[]{"T2"}));
        tm.execute(new Command(CommandType.WRITE, new String[]{"T1", "x1", "101"}));
        tm.execute(new Command(CommandType.WRITE, new String[]{"T2", "x2", "202"}));
        tm.execute(new Command(CommandType.WRITE, new String[]{"T1", "x2", "102"}));
        tm.execute(new Command(CommandType.WRITE, new String[]{"T2", "x1", "201"}));
        tm.execute(new Command(CommandType.END, new String[]{"T2"}));  // T2 commits first
        tm.execute(new Command(CommandType.END, new String[]{"T1"}));  // T1 should abort
        System.out.println();

        System.out.println("Test 3: Snapshot Isolation (from spec Test 2)");
        tm.execute(new Command(CommandType.BEGIN, new String[]{"T3"}));
        tm.execute(new Command(CommandType.BEGIN, new String[]{"T4"}));
        tm.execute(new Command(CommandType.WRITE, new String[]{"T3", "x1", "101"}));
        tm.execute(new Command(CommandType.READ, new String[]{"T4", "x2"}));  // Should read initial value
        tm.execute(new Command(CommandType.WRITE, new String[]{"T3", "x2", "102"}));
        tm.execute(new Command(CommandType.READ, new String[]{"T4", "x1"}));  // Should read initial value
        tm.execute(new Command(CommandType.END, new String[]{"T3"}));
        tm.execute(new Command(CommandType.END, new String[]{"T4"}));
        System.out.println();

        System.out.println("Test 4: Dump");
        tm.execute(new Command(CommandType.DUMP, new String[]{}));
    }
}
