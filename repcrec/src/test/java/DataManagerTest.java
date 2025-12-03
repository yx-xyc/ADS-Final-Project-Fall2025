import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.ads.DataManager;
import com.ads.exceptions.StaleReadException;

public class DataManagerTest {

    @Test
    public void testInitialization() {
        final DataManager dm = new DataManager(2);
        try {
            // x2 is even (replicated), x1 is odd (1 + 1%10 = 2).
            int v2 = dm.read("T1", "x2", 5);
            assertEquals(20, v2);
            
            int v1 = dm.read("T1", "x1", 5);
            assertEquals(10, v1);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testPendingWriteAndCommit() throws Exception {
        DataManager dm = new DataManager(1);
        // Prepare write
        dm.prepareWrite("T1", "x2", 100);
        
        // Read before commit (snapshot time 5) -> Should see initial 20
        int v = dm.read("T2", "x2", 5);
        assertEquals(20, v);
        
        // Commit at time 10
        dm.commit("T1", 10);
        
        // Read after commit (snapshot time 11) -> Should see 100
        v = dm.read("T3", "x2", 11);
        assertEquals(100, v);
    }

    @Test
    public void testStaleReadAfterRecovery() throws Exception {
        DataManager dm = new DataManager(2); // x2 is replicated
        dm.fail();
        dm.recover(20); // Recover at time 20
        
        // T1 starts at 25. x2 has not been written since recovery.
        try {
            dm.read("T1", "x2", 25);
            fail("Should have thrown StaleReadException");
        } catch (StaleReadException e) {
            // Expected
        }
        
        // Write to x2
        dm.prepareWrite("T_write", "x2", 50);
        dm.commit("T_write", 30);
        
        // Now T2 (starts 35) should succeed
        int v = dm.read("T2", "x2", 35);
        assertEquals(50, v);
    }
}
