import java.io.Serializable;
import java.sql.Connection;

/** The WRONG design: the live Connection is an ordinary field. */
public class BadResident implements Serializable {
    public String name, regNo;
    public String password;
    public Connection conn;                   // NOT transient
    public BadResident(String name, String regNo, String password, Connection conn) { this.name = name; this.regNo = regNo; this.password = password; this.conn = conn; }
}
