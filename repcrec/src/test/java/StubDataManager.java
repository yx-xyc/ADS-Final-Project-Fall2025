import com.ads.interfaces.IDataManager;
import java.util.HashMap;
import java.util.Map;

/**
 * Stub DataManager for testing TransactionManager independently.
 * Implements basic in-memory storage with initial values.
 */
public class StubDataManager implements IDataManager {
    private final int siteId;
    private final Map<String, Integer> data;
    private boolean isUp;

    public StubDataManager(int siteId) {
        this.siteId = siteId;
        this.data = new HashMap<>();
        this.isUp = true;

        // Initialize variables with initial values (xi = i * 10)
        for (int i = 1; i <= 20; i++) {
            String varId = "x" + i;
            // Replicated variables (even) on all sites
            if (i % 2 == 0) {
                data.put(varId, i * 10);
            }
            // Non-replicated variables (odd) on specific site
            else if ((1 + (i % 10)) == siteId) {
                data.put(varId, i * 10);
            }
        }
    }

    @Override
    public int read(String transactionId, String variableId, int startTime) throws Exception {
        if (!isUp) {
            throw new Exception("Site " + siteId + " is down");
        }
        if (!data.containsKey(variableId)) {
            throw new Exception("Variable " + variableId + " not found at site " + siteId);
        }
        return data.get(variableId);
    }

    @Override
    public void prepareWrite(String transactionId, String variableId, int value) throws Exception {
        if (!isUp) {
            throw new Exception("Site " + siteId + " is down");
        }
        // For now, just store directly (real implementation would buffer)
        data.put(variableId, value);
    }

    @Override
    public void commit(String transactionId, int commitTime) throws Exception {
        // In real implementation, this would persist buffered writes
        // For stub, writes are already applied in prepareWrite
    }

    @Override
    public void abort(String transactionId) {
        // In real implementation, this would discard buffered writes
        // For stub, nothing to do
    }

    @Override
    public void fail() {
        isUp = false;
    }

    @Override
    public void recover() {
        isUp = true;
    }
}
