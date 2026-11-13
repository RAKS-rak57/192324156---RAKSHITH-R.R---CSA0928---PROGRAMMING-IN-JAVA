import java.io.Serializable;
import java.sql.Connection;

/** A resident of the hostel. password and conn are transient: they must NOT be written to the file. */
public class Resident implements Serializable {
    private static final long serialVersionUID = 1L;
    public String name, regNo;
    public transient String password;          // secret: never stored in the clear on disk (re-entered or stored as a hash elsewhere)
    public transient Connection conn;          // live resource: meaningless after the program ends, and Connection is not Serializable

    public Resident(String name, String regNo, String password, Connection conn) { this.name = name; this.regNo = regNo; this.password = password; this.conn = conn; }
    @Override public String toString() { return name + "(" + regNo + ", pwd=" + password + ", conn=" + (conn == null ? "null" : "live") + ")"; }
}
