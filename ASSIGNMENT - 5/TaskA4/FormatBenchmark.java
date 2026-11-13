import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Alternative of A4: Java serialization  versus  CSV  versus  JSON text, for n residents. */
public class FormatBenchmark {
    static class Res implements Serializable {
        private static final long serialVersionUID = 1L;
        String name, regNo, room, phone;
        Res(String n, String r, String rm, String p) { name = n; regNo = r; room = rm; phone = p; }
    }
    static long sink = 0;
    static List<Res> make(int n) { List<Res> l = new ArrayList<>(n); for (int i = 0; i < n; i++) l.add(new Res("Resident Name " + i, String.format("19%07d", i), "A-" + (100 + i % 400), String.format("98%08d", i))); return l; }

    static void saveSer(List<Res> l, Path f) throws IOException { try (ObjectOutputStream o = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(f)))) { o.writeObject(l); } }
    @SuppressWarnings("unchecked") static List<Res> loadSer(Path f) throws Exception { try (ObjectInputStream i = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(f)))) { return (List<Res>) i.readObject(); } }
    static String csvEsc(String s) { return (s.contains(",") || s.contains("\"")) ? "\"" + s.replace("\"", "\"\"") + "\"" : s; }
    static void saveCsv(List<Res> l, Path f) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(f)) { w.write("name,regNo,room,phone\n"); for (Res r : l) w.write(csvEsc(r.name) + "," + r.regNo + "," + r.room + "," + r.phone + "\n"); }
    }
    static List<Res> loadCsv(Path f) throws IOException {
        List<Res> l = new ArrayList<>();
        try (BufferedReader r = Files.newBufferedReader(f)) { r.readLine(); String s; while ((s = r.readLine()) != null) { String[] p = s.split(",", -1); l.add(new Res(p[0], p[1], p[2], p[3])); } }
        return l;
    }
    static String jsonEsc(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }
    static void saveJson(List<Res> l, Path f) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(f)) {
            w.write("[\n");
            for (int i = 0; i < l.size(); i++) { Res r = l.get(i); w.write("{\"name\":\"" + jsonEsc(r.name) + "\",\"regNo\":\"" + r.regNo + "\",\"room\":\"" + r.room + "\",\"phone\":\"" + r.phone + "\"}" + (i + 1 < l.size() ? ",\n" : "\n")); }
            w.write("]\n");
        }
    }
    static String field(String line, String key) { int i = line.indexOf("\"" + key + "\":\""); if (i < 0) return null; i += key.length() + 4; return line.substring(i, line.indexOf('"', i)); }
    static List<Res> loadJson(Path f) throws IOException {
        List<Res> l = new ArrayList<>();
        try (BufferedReader r = Files.newBufferedReader(f)) { String s; while ((s = r.readLine()) != null) if (s.startsWith("{")) l.add(new Res(field(s, "name"), field(s, "regNo"), field(s, "room"), field(s, "phone"))); }
        return l;
    }

    public static void main(String[] args) throws Exception {
        PrintWriter csv = new PrintWriter("format_results.csv");
        csv.println("n,format,file_bytes,save_ms,load_ms");
        System.out.println("=== Serialization vs CSV vs JSON for n residents (best of 5 runs) ===");
        System.out.printf("%9s %-14s %14s %12s %12s%n", "n", "format", "file bytes", "save (ms)", "load (ms)");
        for (int w = 0; w < 3; w++) { List<Res> l = make(5000); Path f = Files.createTempFile("w", ".x"); saveSer(l, f); loadSer(f); saveCsv(l, f); loadCsv(f); saveJson(l, f); loadJson(f); Files.delete(f); }
        for (int n : new int[]{1000, 10000, 100000}) {
            List<Res> data = make(n);
            for (String fmt : new String[]{"serialization", "CSV", "JSON"}) {
                Path f = Files.createTempFile("res", ".x");
                double save = 1e18, load = 1e18; int size = 0;
                for (int r = 0; r < 5; r++) {
                    long t = System.nanoTime();
                    if (fmt.equals("serialization")) saveSer(data, f); else if (fmt.equals("CSV")) saveCsv(data, f); else saveJson(data, f);
                    save = Math.min(save, (System.nanoTime() - t) / 1e6);
                    size = (int) Files.size(f);
                    t = System.nanoTime();
                    List<Res> back = fmt.equals("serialization") ? loadSer(f) : fmt.equals("CSV") ? loadCsv(f) : loadJson(f);
                    load = Math.min(load, (System.nanoTime() - t) / 1e6);
                    if (back.size() != n || !back.get(n - 1).name.equals(data.get(n - 1).name)) throw new IllegalStateException("round trip failed for " + fmt);
                }
                System.out.printf("%,9d %-14s %,14d %12.1f %12.1f%n", n, fmt, size, save, load);
                csv.printf("%d,%s,%d,%.2f,%.2f%n", n, fmt, size, save, load);
                Files.delete(f);
            }
        }
        csv.close();

        System.out.println();
        System.out.println("=== A NEW FIELD (building) is added after files were saved: old file read by the new program ===");
        String oldCsv = "name,regNo,room,phone\nArun,192311001,A-101,9800000001\n";
        String[] header = oldCsv.split("\n")[0].split(","), row = oldCsv.split("\n")[1].split(",");
        try { String building = row[4]; System.out.println("CSV, positional reader expecting 5 columns : " + building); }
        catch (ArrayIndexOutOfBoundsException e) { System.out.println("CSV, positional reader expecting 5 columns : ArrayIndexOutOfBoundsException (" + e.getMessage() + ")"); }
        Map<String, String> byName = new HashMap<>(); for (int i = 0; i < header.length; i++) byName.put(header[i], row[i]);
        System.out.println("CSV, header-aware reader (looks up 'building')      : " + byName.getOrDefault("building", "(missing -> default)") + "  (works, if the reader is written that way)");
        String oldJson = "{\"name\":\"Arun\",\"regNo\":\"192311001\",\"room\":\"A-101\",\"phone\":\"9800000001\"}";
        System.out.println("JSON, reader looks up the key 'building'            : " + field(oldJson, "building") + "  (missing key -> default; extra keys in the file are ignored)");
        System.out.println("Java serialization with fixed serialVersionUID      : new field = default value; WITHOUT a fixed UID: InvalidClassException (see versioning output)");
        System.out.println("(checksum " + sink + ")");
    }
}
