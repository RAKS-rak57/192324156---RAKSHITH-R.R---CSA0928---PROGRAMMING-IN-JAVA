import java.io.Serializable;
public class ResidentFixed implements Serializable {
    private static final long serialVersionUID = 1L;              // same number: "this is still the same kind of object"
    public String name, regNo;
    public String phone;                                          // NEW FIELD
    public ResidentFixed(String n, String r) { name = n; regNo = r; }
}
