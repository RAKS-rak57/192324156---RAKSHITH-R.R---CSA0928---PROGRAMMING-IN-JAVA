import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Hostel implements Serializable {
    private static final long serialVersionUID = 1L;
    public String name;
    public List<Room> rooms = new ArrayList<>();
    public Hostel(String name) { this.name = name; }
}
