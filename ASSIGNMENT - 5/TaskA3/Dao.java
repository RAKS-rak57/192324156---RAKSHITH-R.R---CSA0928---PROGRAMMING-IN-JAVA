import java.sql.SQLException;
import java.util.List;

/** Generic data-access interface: T = the entity class, K = the type of its primary key. */
public interface Dao<T, K> {
    void insert(T entity) throws SQLException;
    T findById(K key) throws SQLException;          // null when there is no such row
    List<T> findAll() throws SQLException;
    int update(T entity) throws SQLException;       // number of rows changed
    int delete(K key) throws SQLException;          // number of rows deleted
}
