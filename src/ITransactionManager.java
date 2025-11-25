public interface ITransactionManager {
    void begin(String txnId);
    void read(String txnId, String varId);
    void write(String txnId, String varId, int value);
    void end(String txnId);
    void dump();
    void fail(int siteId);
    void recover(int siteId);
}
