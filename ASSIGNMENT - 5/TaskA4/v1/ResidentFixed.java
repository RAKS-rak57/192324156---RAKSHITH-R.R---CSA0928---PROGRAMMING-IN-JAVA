import java.io.Serializable;
public class ResidentFixed implements Serializable {
    private static final long serialVersionUID = 1L;              // explicit version number
    public String name, regNo;
    public ResidentFixed(String n, String r) { name = n; regNo = r; }
}
