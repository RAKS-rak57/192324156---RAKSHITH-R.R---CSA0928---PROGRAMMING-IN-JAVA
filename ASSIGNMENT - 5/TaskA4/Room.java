import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Room implements Serializable {
    private static final long serialVersionUID = 1L;
    public int number;
    public List<Resident> residents = new ArrayList<>();
    public Room(int number) { this.number = number; }
}
