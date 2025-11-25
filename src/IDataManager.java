public interface IDataManager {
    int read(String transactionId, String variableId, int startTime) throws Exception;
    void prepareWrite(String transactionId, String variableId, int value) throws Exception;
    void commit(String transactionId, int commitTime) throws Exception;
    void abort(String transactionId);
    void fail();
    void recover();
}
