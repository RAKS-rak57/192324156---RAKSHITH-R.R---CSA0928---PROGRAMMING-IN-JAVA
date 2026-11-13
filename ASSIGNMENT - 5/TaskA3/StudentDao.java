import java.sql.*;

/** Object -> table mapping:  regNo -> reg_no (PRIMARY KEY), name -> name, email -> email, year -> year_no. */
public class StudentDao extends AbstractJdbcDao<Student, String> {
    public StudentDao(ConnectionFactory f) { super(f); }
    @Override protected String insertSql()     { return "INSERT INTO student(reg_no, name, email, year_no) VALUES (?, ?, ?, ?)"; }
    @Override protected String selectByIdSql() { return "SELECT reg_no, name, email, year_no FROM student WHERE reg_no = ?"; }
    @Override protected String selectAllSql()  { return "SELECT reg_no, name, email, year_no FROM student ORDER BY reg_no"; }
    @Override protected String updateSql()     { return "UPDATE student SET name = ?, email = ?, year_no = ? WHERE reg_no = ?"; }
    @Override protected String deleteSql()     { return "DELETE FROM student WHERE reg_no = ?"; }
    @Override protected void bindInsert(PreparedStatement ps, Student s) throws SQLException { ps.setString(1, s.regNo); ps.setString(2, s.name); ps.setString(3, s.email); ps.setInt(4, s.year); }
    @Override protected void bindUpdate(PreparedStatement ps, Student s) throws SQLException { ps.setString(1, s.name); ps.setString(2, s.email); ps.setInt(3, s.year); ps.setString(4, s.regNo); }
    @Override protected void bindKey(PreparedStatement ps, String key) throws SQLException { ps.setString(1, key); }
    @Override protected Student mapRow(ResultSet rs) throws SQLException { return new Student(rs.getString("reg_no"), rs.getString("name"), rs.getString("email"), rs.getInt("year_no")); }
}
