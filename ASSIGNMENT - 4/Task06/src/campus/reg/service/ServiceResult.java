package campus.reg.service;

public class ServiceResult {
    public final boolean success;
    public final String message;
    private ServiceResult(boolean s, String m) { success = s; message = m; }
    public static ServiceResult ok(String m) { return new ServiceResult(true, m); }
    public static ServiceResult fail(String m) { return new ServiceResult(false, m); }
}
