import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The steps that were copied into StudentDB, CourseDB and MarksDB are written ONCE here:
 * open connection -> prepare SQL -> bind parameters -> execute -> map rows to objects -> close everything.
 * A subclass supplies only what differs: the SQL text, how to bind an entity, and how to turn a row into an object.
 */
public abstract class AbstractJdbcDao<T, K> implements Dao<T, K> {
    public interface ConnectionFactory { Connection open() throws SQLException; }
    public interface Binder { void bind(PreparedStatement ps) throws SQLException; }
    public interface RowMapper<R> { R map(ResultSet rs) throws SQLException; }

    private final ConnectionFactory connections;
    protected AbstractJdbcDao(ConnectionFactory connections) { this.connections = connections; }

    // ----- the parts that differ (one small method each) -----
    protected abstract String insertSql();
    protected abstract void bindInsert(PreparedStatement ps, T e) throws SQLException;
    protected abstract String selectByIdSql();
    protected abstract String selectAllSql();
    protected abstract String updateSql();
    protected abstract void bindUpdate(PreparedStatement ps, T e) throws SQLException;
    protected abstract String deleteSql();
    protected abstract T mapRow(ResultSet rs) throws SQLException;
    protected abstract void bindKey(PreparedStatement ps, K key) throws SQLException;

    // ----- the repeated steps, written once -----
    protected <R> R query(String sql, Binder binder, RowMapper<R> mapper, boolean many, List<R> into) throws SQLException {
        try (Connection c = connections.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                R first = null;
                while (rs.next()) { R r = mapper.map(rs); if (first == null) first = r; if (many) into.add(r); else break; }
                return first;
            }
        }
    }
    protected int execute(String sql, Binder binder) throws SQLException {
        try (Connection c = connections.open(); PreparedStatement ps = c.prepareStatement(sql)) { binder.bind(ps); return ps.executeUpdate(); }
    }

    @Override public void insert(T e) throws SQLException { execute(insertSql(), ps -> bindInsert(ps, e)); }
    @Override public T findById(K key) throws SQLException { return query(selectByIdSql(), ps -> bindKey(ps, key), this::mapRow, false, null); }
    @Override public List<T> findAll() throws SQLException { List<T> all = new ArrayList<>(); query(selectAllSql(), ps -> { }, this::mapRow, true, all); return all; }
    @Override public int update(T e) throws SQLException { return execute(updateSql(), ps -> bindUpdate(ps, e)); }
    @Override public int delete(K key) throws SQLException { return execute(deleteSql(), ps -> bindKey(ps, key)); }
}
