import java.io.Serializable;
public class ResidentImplicit implements Serializable {           // NO serialVersionUID: the compiler computes one from the class structure
    public String name, regNo;
    public ResidentImplicit(String n, String r) { name = n; regNo = r; }
}
