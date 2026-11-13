import java.sql.*;

public class CourseDao extends AbstractJdbcDao<CourseDao.Course, String> {
    public static class Course { public String code, title; public int credits; public Course(String c, String t, int cr) { code = c; title = t; credits = cr; } public String toString() { return code + " " + title + " (" + credits + " credits)"; } }
    public CourseDao(ConnectionFactory f) { super(f); }
    @Override protected String insertSql()     { return "INSERT INTO course(course_code, title, credits) VALUES (?, ?, ?)"; }
    @Override protected String selectByIdSql() { return "SELECT course_code, title, credits FROM course WHERE course_code = ?"; }
    @Override protected String selectAllSql()  { return "SELECT course_code, title, credits FROM course ORDER BY course_code"; }
    @Override protected String updateSql()     { return "UPDATE course SET title = ?, credits = ? WHERE course_code = ?"; }
    @Override protected String deleteSql()     { return "DELETE FROM course WHERE course_code = ?"; }
    @Override protected void bindInsert(PreparedStatement ps, Course c) throws SQLException { ps.setString(1, c.code); ps.setString(2, c.title); ps.setInt(3, c.credits); }
    @Override protected void bindUpdate(PreparedStatement ps, Course c) throws SQLException { ps.setString(1, c.title); ps.setInt(2, c.credits); ps.setString(3, c.code); }
    @Override protected void bindKey(PreparedStatement ps, String k) throws SQLException { ps.setString(1, k); }
    @Override protected Course mapRow(ResultSet rs) throws SQLException { return new Course(rs.getString(1), rs.getString(2), rs.getInt(3)); }
}
