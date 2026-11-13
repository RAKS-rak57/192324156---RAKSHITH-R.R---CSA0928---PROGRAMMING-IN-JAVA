package minisqlite;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;

/**
 * TEST-ONLY JDBC driver ("jdbc:minisqlite:<file or :memory:>") that talks to the real SQLite library through Java's
 * foreign-function API. It exists because no MySQL server / Connector-J jar was available while preparing this assignment.
 * Application code uses ONLY java.sql interfaces, so for MySQL only the JDBC URL and the driver jar change.
 * Only the JDBC methods needed by the assignment programs are implemented; any other method throws SQLFeatureNotSupportedException.
 * Run with:  --enable-preview --enable-native-access=ALL-UNNAMED   (Java 21)
 */
public class MiniSqlite {
    public static final String PREFIX = "jdbc:minisqlite:";

    // ------------------------------------------------------------------ native layer
    static final Linker LINKER = Linker.nativeLinker();
    static final SymbolLookup LIB = SymbolLookup.libraryLookup("libsqlite3.so.0", Arena.global());
    static MethodHandle fn(String name, MemoryLayout ret, MemoryLayout... args) {
        FunctionDescriptor d = ret == null ? FunctionDescriptor.ofVoid(args) : FunctionDescriptor.of(ret, args);
        return LINKER.downcallHandle(LIB.find(name).orElseThrow(), d);
    }
    static final ValueLayout.OfInt I = ValueLayout.JAVA_INT;
    static final ValueLayout.OfLong L = ValueLayout.JAVA_LONG;
    static final ValueLayout.OfDouble D = ValueLayout.JAVA_DOUBLE;
    static final AddressLayout A = ValueLayout.ADDRESS;
    static final MethodHandle OPEN = fn("sqlite3_open", I, A, A), CLOSE = fn("sqlite3_close", I, A), EXEC = fn("sqlite3_exec", I, A, A, A, A, A),
        PREPARE = fn("sqlite3_prepare_v2", I, A, A, I, A, A), STEP = fn("sqlite3_step", I, A), RESET = fn("sqlite3_reset", I, A),
        FINALIZE = fn("sqlite3_finalize", I, A), CLEAR = fn("sqlite3_clear_bindings", I, A), ERRMSG = fn("sqlite3_errmsg", A, A),
        BIND_TEXT = fn("sqlite3_bind_text", I, A, I, A, I, A), BIND_LONG = fn("sqlite3_bind_int64", I, A, I, L),
        BIND_DOUBLE = fn("sqlite3_bind_double", I, A, I, D), BIND_NULL = fn("sqlite3_bind_null", I, A, I),
        COL_COUNT = fn("sqlite3_column_count", I, A), COL_NAME = fn("sqlite3_column_name", A, A, I), COL_DECL = fn("sqlite3_column_decltype", A, A, I),
        COL_TEXT = fn("sqlite3_column_text", A, A, I), COL_LONG = fn("sqlite3_column_int64", L, A, I), COL_DOUBLE = fn("sqlite3_column_double", D, A, I),
        COL_TYPE = fn("sqlite3_column_type", I, A, I), CHANGES = fn("sqlite3_changes", I, A), LAST_ID = fn("sqlite3_last_insert_rowid", L, A),
        VERSION = fn("sqlite3_libversion", A);
    static final MemorySegment TRANSIENT = MemorySegment.ofAddress(-1);

    static String str(MemorySegment p) { return p.address() == 0 ? null : p.reinterpret(Long.MAX_VALUE).getUtf8String(0); }
    static RuntimeException rt(Throwable t) { return t instanceof RuntimeException r ? r : new RuntimeException(t); }

    public static String libVersion() { try { return str((MemorySegment) VERSION.invoke()); } catch (Throwable t) { throw rt(t); } }

    static class Db {
        MemorySegment db;
        Db(String path) throws SQLException {
            try (Arena a = Arena.ofConfined()) {
                MemorySegment cell = a.allocate(A);
                int rc = (int) OPEN.invoke(a.allocateUtf8String(path), cell);
                db = cell.get(A, 0);
                if (rc != 0) throw new SQLException("cannot open " + path);
            } catch (SQLException e) { throw e; } catch (Throwable t) { throw rt(t); }
        }
        SQLException error(int rc, String sql) {
            try {
                String m = str((MemorySegment) ERRMSG.invoke(db));
                if ((rc & 0xff) == 19) return new SQLIntegrityConstraintViolationException(m, "23000", rc);
                return new SQLException(m + (sql == null ? "" : "  [" + abbreviate(sql) + "]"), "HY000", rc);
            } catch (Throwable t) { throw rt(t); }
        }
        static String abbreviate(String s) { return s.length() > 90 ? s.substring(0, 90) + "..." : s; }
        int exec(String sql) throws SQLException {            // full parse + execute every time (what a JDBC Statement does)
            try (Arena a = Arena.ofConfined()) {
                int rc = (int) EXEC.invoke(db, a.allocateUtf8String(sql), MemorySegment.NULL, MemorySegment.NULL, MemorySegment.NULL);
                if (rc != 0) throw error(rc, sql);
                return (int) CHANGES.invoke(db);
            } catch (SQLException e) { throw e; } catch (Throwable t) { throw rt(t); }
        }
        MemorySegment prepare(String sql) throws SQLException {
            try (Arena a = Arena.ofConfined()) {
                MemorySegment cell = a.allocate(A);
                int rc = (int) PREPARE.invoke(db, a.allocateUtf8String(sql), -1, cell, MemorySegment.NULL);
                if (rc != 0) throw error(rc, sql);
                return cell.get(A, 0);
            } catch (SQLException e) { throw e; } catch (Throwable t) { throw rt(t); }
        }
        int step(MemorySegment st, String sql) throws SQLException {
            try { int rc = (int) STEP.invoke(st); if (rc != 100 && rc != 101) throw error(rc, sql); return rc; }
            catch (SQLException e) { throw e; } catch (Throwable t) { throw rt(t); }
        }
        void close() { try { CLOSE.invoke(db); } catch (Throwable t) { throw rt(t); } }
    }

    // ------------------------------------------------------------------ JDBC proxies
    @SuppressWarnings("unchecked")
    static <T> T proxy(Class<T> c, InvocationHandler h) { return (T) Proxy.newProxyInstance(MiniSqlite.class.getClassLoader(), new Class<?>[]{c}, h); }
    static Object objectMethod(Object self, Method m, Object[] a, String name) {
        switch (m.getName()) {
            case "toString": return name + "@" + Integer.toHexString(System.identityHashCode(self));
            case "hashCode": return System.identityHashCode(self);
            case "equals": return self == a[0];
            default: return null;
        }
    }
    static boolean isObjectMethod(Method m) { return m.getDeclaringClass() == Object.class; }
    static SQLFeatureNotSupportedException unsupported(Method m) { return new SQLFeatureNotSupportedException("MiniSqlite does not implement " + m.getDeclaringClass().getSimpleName() + "." + m.getName()); }

    /** Registers the driver with java.sql.DriverManager. */
    public static void install() throws SQLException {
        Driver d = proxy(Driver.class, (self, m, a) -> {
            if (isObjectMethod(m)) return objectMethod(self, m, a, "MiniSqliteDriver");
            switch (m.getName()) {
                case "acceptsURL": return a[0] != null && ((String) a[0]).startsWith(PREFIX);
                case "connect": return ((String) a[0]).startsWith(PREFIX) ? new Conn(((String) a[0]).substring(PREFIX.length())).jdbc() : null;
                case "getMajorVersion": return 0; case "getMinorVersion": return 1; case "jdbcCompliant": return false;
                default: throw unsupported(m);
            }
        });
        DriverManager.registerDriver(d);
    }

    static class Conn {
        final Db db; boolean autoCommit = true, inTx = false, closed = false; Connection jdbc;
        Conn(String path) throws SQLException { db = new Db(path); }
        Connection jdbc() {
            jdbc = proxy(Connection.class, (self, m, a) -> {
                if (isObjectMethod(m)) return objectMethod(self, m, a, "MiniSqliteConnection");
                switch (m.getName()) {
                    case "createStatement": return new Stmt(this).jdbc();
                    case "prepareStatement": return new Prep(this, (String) a[0]).jdbc();
                    case "setAutoCommit": {
                        boolean on = (Boolean) a[0];
                        if (on && inTx) { db.exec("COMMIT"); inTx = false; }
                        if (!on && !inTx) { db.exec("BEGIN"); inTx = true; }
                        autoCommit = on; return null; }
                    case "getAutoCommit": return autoCommit;
                    case "commit": if (inTx) { db.exec("COMMIT"); inTx = false; if (!autoCommit) { db.exec("BEGIN"); inTx = true; } } return null;
                    case "rollback": if (inTx) { db.exec("ROLLBACK"); inTx = false; if (!autoCommit) { db.exec("BEGIN"); inTx = true; } } return null;
                    case "close": if (!closed) { if (inTx) { try { db.exec("ROLLBACK"); } catch (SQLException ignore) { } } db.close(); closed = true; } return null;
                    case "isClosed": return closed;
                    case "getMetaData": return meta();
                    case "isValid": return !closed;
                    default: throw unsupported(m);
                }
            });
            return jdbc;
        }
        DatabaseMetaData meta() {
            return proxy(DatabaseMetaData.class, (self, m, a) -> {
                if (isObjectMethod(m)) return objectMethod(self, m, a, "MiniSqliteMetaData");
                switch (m.getName()) {
                    case "getDatabaseProductName": return "SQLite (via MiniSqlite test driver)";
                    case "getDatabaseProductVersion": return libVersion();
                    case "getDriverName": return "MiniSqlite JDBC test driver (java.lang.foreign)";
                    case "getDriverVersion": return "0.1";
                    case "getURL": return PREFIX + "...";
                    case "getUserName": return "(none)";
                    case "getTables": {
                        PreparedStatement ps = new Prep(this, "SELECT NULL AS TABLE_CAT, NULL AS TABLE_SCHEM, name AS TABLE_NAME, 'TABLE' AS TABLE_TYPE FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' ORDER BY name").jdbc();
                        return ps.executeQuery(); }
                    default: throw unsupported(m);
                }
            });
        }
    }

    static class Stmt {
        final Conn c; final List<String> batch = new ArrayList<>(); boolean closed;
        Stmt(Conn c) { this.c = c; }
        Statement jdbc() {
            return proxy(Statement.class, (self, m, a) -> {
                if (isObjectMethod(m)) return objectMethod(self, m, a, "MiniSqliteStatement");
                switch (m.getName()) {
                    case "executeUpdate": return c.db.exec((String) a[0]);
                    case "execute": { String sql = (String) a[0]; c.db.exec(sql); return false; }
                    case "executeQuery": return new Rs(c, c.db.prepare((String) a[0]), (String) a[0], true).jdbc();
                    case "addBatch": batch.add((String) a[0]); return null;
                    case "clearBatch": batch.clear(); return null;
                    case "executeBatch": { int[] r = new int[batch.size()]; for (int i = 0; i < r.length; i++) r[i] = c.db.exec(batch.get(i)); batch.clear(); return r; }
                    case "getConnection": return c.jdbc;
                    case "close": closed = true; return null;
                    case "isClosed": return closed;
                    default: throw unsupported(m);
                }
            });
        }
    }

    static class Prep {
        final Conn c; final String sql; final MemorySegment st; final int nParams; final Object[] params;
        final List<Object[]> batch = new ArrayList<>(); boolean closed;
        Prep(Conn c, String sql) throws SQLException {
            this.c = c; this.sql = sql; this.st = c.db.prepare(sql);
            int n = 0; for (char ch : sql.toCharArray()) if (ch == '?') n++;
            nParams = n; params = new Object[n + 1];
        }
        void bindAll(Object[] p) throws SQLException {
            try {
                RESET.invoke(st); CLEAR.invoke(st);
                for (int i = 1; i <= nParams; i++) {
                    Object v = p[i];
                    int rc;
                    if (v == null) rc = (int) BIND_NULL.invoke(st, i);
                    else if (v instanceof Integer || v instanceof Long) rc = (int) BIND_LONG.invoke(st, i, ((Number) v).longValue());
                    else if (v instanceof Double || v instanceof Float) rc = (int) BIND_DOUBLE.invoke(st, i, ((Number) v).doubleValue());
                    else try (Arena a = Arena.ofConfined()) { rc = (int) BIND_TEXT.invoke(st, i, a.allocateUtf8String(v.toString()), -1, TRANSIENT); }
                    if (rc != 0) throw c.db.error(rc, sql);
                }
            } catch (SQLException e) { throw e; } catch (Throwable t) { throw rt(t); }
        }
        int runUpdate(Object[] p) throws SQLException {
            bindAll(p);
            try { c.db.step(st, sql); return (int) CHANGES.invoke(c.db.db); }
            catch (SQLException e) { try { RESET.invoke(st); } catch (Throwable ignore) { } throw e; }
            catch (Throwable t) { throw rt(t); }
        }
        PreparedStatement jdbc() {
            return proxy(PreparedStatement.class, (self, m, a) -> {
                if (isObjectMethod(m)) return objectMethod(self, m, a, "MiniSqlitePreparedStatement");
                String n = m.getName();
                switch (n) {
                    case "setString": case "setInt": case "setLong": case "setDouble": case "setObject": case "setBigDecimal": case "setFloat": {
                        int idx = (Integer) a[0]; if (idx < 1 || idx > nParams) throw new SQLException("parameter index out of range: " + idx);
                        params[idx] = a[1]; return null; }
                    case "setNull": params[(Integer) a[0]] = null; return null;
                    case "clearParameters": Arrays.fill(params, null); return null;
                    case "executeUpdate": return runUpdate(params);
                    case "execute": runUpdate(params); return false;
                    case "executeQuery": { bindAll(params); return new Rs(c, st, sql, false).jdbc(); }
                    case "addBatch": batch.add(params.clone()); return null;
                    case "clearBatch": batch.clear(); return null;
                    case "executeBatch": { int[] r = new int[batch.size()]; try { for (int i = 0; i < r.length; i++) r[i] = runUpdate(batch.get(i)); } finally { batch.clear(); } return r; }
                    case "getGeneratedKeys": return new Prep(c, "SELECT last_insert_rowid() AS GENERATED_KEY").jdbc().executeQuery();
                    case "getConnection": return c.jdbc;
                    case "close": if (!closed) { closed = true; FINALIZE.invoke(st); } return null;
                    case "isClosed": return closed;
                    default: throw unsupported(m);
                }
            });
        }
    }

    static class Rs {
        final Conn c; final MemorySegment st; final String sql; final boolean ownsStmt; boolean done, closed, wasNull;
        Rs(Conn c, MemorySegment st, String sql, boolean ownsStmt) { this.c = c; this.st = st; this.sql = sql; this.ownsStmt = ownsStmt; }
        int colIndex(Object o) throws Throwable {
            if (o instanceof Integer i) return i - 1;
            int n = (int) COL_COUNT.invoke(st);
            for (int i = 0; i < n; i++) if (str((MemorySegment) COL_NAME.invoke(st, i)).equalsIgnoreCase((String) o)) return i;
            throw new SQLException("no such column: " + o);
        }
        ResultSet jdbc() {
            return proxy(ResultSet.class, (self, m, a) -> {
                if (isObjectMethod(m)) return objectMethod(self, m, a, "MiniSqliteResultSet");
                switch (m.getName()) {
                    case "next": { if (done) return false; int rc = c.db.step(st, sql); if (rc == 101) { done = true; return false; } return true; }
                    case "getString": { MemorySegment p = (MemorySegment) COL_TEXT.invoke(st, colIndex(a[0])); wasNull = p.address() == 0; return str(p); }
                    case "getInt": { long v = (long) COL_LONG.invoke(st, colIndex(a[0])); return (int) v; }
                    case "getLong": return (long) COL_LONG.invoke(st, colIndex(a[0]));
                    case "getDouble": return (double) COL_DOUBLE.invoke(st, colIndex(a[0]));
                    case "wasNull": return wasNull;
                    case "getMetaData": return meta();
                    case "close": if (!closed) { closed = true; if (ownsStmt) FINALIZE.invoke(st); else RESET.invoke(st); } return null;
                    case "isClosed": return closed;
                    default: throw unsupported(m);
                }
            });
        }
        ResultSetMetaData meta() {
            return proxy(ResultSetMetaData.class, (self, m, a) -> {
                if (isObjectMethod(m)) return objectMethod(self, m, a, "MiniSqliteResultSetMetaData");
                switch (m.getName()) {
                    case "getColumnCount": return (int) COL_COUNT.invoke(st);
                    case "getColumnName": case "getColumnLabel": return str((MemorySegment) COL_NAME.invoke(st, (Integer) a[0] - 1));
                    case "getColumnTypeName": { String d = str((MemorySegment) COL_DECL.invoke(st, (Integer) a[0] - 1)); return d == null ? "(expression)" : d; }
                    case "getColumnType": { String d = str((MemorySegment) COL_DECL.invoke(st, (Integer) a[0] - 1)); d = d == null ? "" : d.toUpperCase();
                        return d.startsWith("INT") ? Types.INTEGER : d.startsWith("DECIMAL") ? Types.DECIMAL : d.startsWith("DOUBLE") ? Types.DOUBLE : Types.VARCHAR; }
                    default: throw unsupported(m);
                }
            });
        }
    }
}
