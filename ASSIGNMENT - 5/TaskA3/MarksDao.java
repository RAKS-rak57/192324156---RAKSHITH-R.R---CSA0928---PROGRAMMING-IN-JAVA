import java.sql.*;

public class MarksDao extends AbstractJdbcDao<MarksDao.Mark, Integer> {
    public static class Mark { public int id; public String regNo, courseCode; public int marks; public Mark(int i, String r, String c, int m) { id = i; regNo = r; courseCode = c; marks = m; } public String toString() { return "#" + id + " " + regNo + " " + courseCode + " = " + marks; } }
    public MarksDao(ConnectionFactory f) { super(f); }
    @Override protected String insertSql()     { return "INSERT INTO marks(mark_id, reg_no, course_code, marks) VALUES (?, ?, ?, ?)"; }
    @Override protected String selectByIdSql() { return "SELECT mark_id, reg_no, course_code, marks FROM marks WHERE mark_id = ?"; }
    @Override protected String selectAllSql()  { return "SELECT mark_id, reg_no, course_code, marks FROM marks ORDER BY mark_id"; }
    @Override protected String updateSql()     { return "UPDATE marks SET reg_no = ?, course_code = ?, marks = ? WHERE mark_id = ?"; }
    @Override protected String deleteSql()     { return "DELETE FROM marks WHERE mark_id = ?"; }
    @Override protected void bindInsert(PreparedStatement ps, Mark m) throws SQLException { ps.setInt(1, m.id); ps.setString(2, m.regNo); ps.setString(3, m.courseCode); ps.setInt(4, m.marks); }
    @Override protected void bindUpdate(PreparedStatement ps, Mark m) throws SQLException { ps.setString(1, m.regNo); ps.setString(2, m.courseCode); ps.setInt(3, m.marks); ps.setInt(4, m.id); }
    @Override protected void bindKey(PreparedStatement ps, Integer k) throws SQLException { ps.setInt(1, k); }
    @Override protected Mark mapRow(ResultSet rs) throws SQLException { return new Mark(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getInt(4)); }
}
