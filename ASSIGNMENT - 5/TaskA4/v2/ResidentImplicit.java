import java.io.Serializable;
public class ResidentImplicit implements Serializable {
    public String name, regNo;
    public String phone;                                          // NEW FIELD (changes the computed serialVersionUID)
    public ResidentImplicit(String n, String r) { name = n; regNo = r; }
}
