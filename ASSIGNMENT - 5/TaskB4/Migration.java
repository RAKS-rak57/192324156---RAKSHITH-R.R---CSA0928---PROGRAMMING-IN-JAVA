import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

/**
 * Task B4.1: reads the existing pass file, validates every line, and loads the good records into bus_pass_full
 * in batches of 1,000 inside ONE transaction. Lines that fail validation are collected in a failure report.
 */
public class Migration {
    static final String INSERT = "INSERT INTO bus_pass_full(pass_id, reg_no, name, route_no, boarding_stop, validity_date, amount, campus_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    public static MigrationReport run(Path passFile, Connection con, int campusId) throws IOException, SQLException {
        MigrationReport rep = new MigrationReport();
        long t0 = System.nanoTime();
        Set<String> seen = new HashSet<>();                                    // duplicate pass ids would violate the PRIMARY KEY
        boolean old = con.getAutoCommit();
        con.setAutoCommit(false);
        try (BufferedReader r = Files.newBufferedReader(passFile, StandardCharsets.UTF_8); PreparedStatement ps = con.prepareStatement(INSERT)) {
            String line; int lineNo = 0, inBatch = 0;
            while ((line = r.readLine()) != null) {
                lineNo++; rep.linesRead++;
                BusPass p;
                try { p = BusPass.fromLine(line); } catch (DamagedRecordException e) { rep.reject(lineNo, e.getMessage(), line); continue; }
                String tooLong = tooLongForColumns(p);
                if (tooLong != null) { rep.reject(lineNo, tooLong, line); continue; }
                if (!seen.add(p.passId)) { rep.duplicates++; rep.reject(lineNo, "duplicate pass id " + p.passId, line); continue; }
                ps.setString(1, p.passId); ps.setString(2, p.regNo); ps.setString(3, p.name); ps.setInt(4, p.route);
                ps.setString(5, p.stop); ps.setString(6, p.validity.toString()); ps.setDouble(7, p.amount); ps.setInt(8, campusId);
                ps.addBatch(); rep.loaded++;
                if (++inBatch == 1000) { ps.executeBatch(); inBatch = 0; }
            }
            ps.executeBatch();
            con.commit();                                                      // all good records become permanent together
        } catch (SQLException | IOException | RuntimeException e) {
            con.rollback();                                                    // nothing is half-loaded
            throw e;
        } finally { con.setAutoCommit(old); }
        rep.millis = (System.nanoTime() - t0) / 1_000_000;
        return rep;
    }

    /** the table columns are VARCHAR(n): a longer value would be cut or rejected by the database, so it is reported here */
    static String tooLongForColumns(BusPass p) {
        if (p.passId.length() > 10) return "pass id longer than 10 characters";
        if (p.regNo.length() > 12) return "register number longer than 12 characters";
        if (p.name.length() > 60) return "name longer than 60 characters";
        if (p.stop.length() > 30) return "boarding stop longer than 30 characters";
        return null;
    }
}
