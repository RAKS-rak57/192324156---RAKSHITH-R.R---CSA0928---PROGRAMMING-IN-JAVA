import java.io.*;
import java.nio.file.*;

/** The line counters of Task A2: from one-byte-at-a-time to buffered, plus the "read everything" alternative. */
public class LineCounter {

    /** Counts the read calls that reach the stream below it (for FileInputStream every call is one operating-system read). */
    public static class CountingInputStream extends FilterInputStream {
        public long calls = 0;
        public CountingInputStream(InputStream in) { super(in); }
        @Override public int read() throws IOException { calls++; return super.read(); }
        @Override public int read(byte[] b, int off, int len) throws IOException { calls++; return in.read(b, off, len); }
    }

    /** (1) ORIGINAL: FileInputStream.read() one byte at a time: one operating-system call per byte. */
    public static long unbuffered(Path file, long[] readCalls) throws IOException {
        long lines = 0;
        try (CountingInputStream in = new CountingInputStream(new FileInputStream(file.toFile()))) {
            int b;
            while ((b = in.read()) != -1) if (b == '\n') lines++;
            readCalls[0] = in.calls;
        }
        return lines;
    }

    /** (2) BufferedInputStream: the program still calls read() per byte, but the stream below is read 8,192 bytes at a time. */
    public static long bufferedStream(Path file, long[] readCalls) throws IOException {
        long lines = 0;
        try (CountingInputStream raw = new CountingInputStream(new FileInputStream(file.toFile()));
             BufferedInputStream in = new BufferedInputStream(raw)) {
            int b;
            while ((b = in.read()) != -1) if (b == '\n') lines++;
            readCalls[0] = raw.calls;
        }
        return lines;
    }

    /** (3) Own byte[] buffer: read(byte[]) fills 64 KB at a time. */
    public static long byteArray(Path file, int bufSize, long[] readCalls) throws IOException {
        long lines = 0;
        byte[] buf = new byte[bufSize];
        try (CountingInputStream in = new CountingInputStream(new FileInputStream(file.toFile()))) {
            int n;
            while ((n = in.read(buf, 0, buf.length)) != -1) for (int i = 0; i < n; i++) if (buf[i] == '\n') lines++;
            readCalls[0] = in.calls;
        }
        return lines;
    }

    /** (4) BufferedReader.readLine(): reads characters, builds a String per line. */
    public static long bufferedReader(Path file) throws IOException {
        long lines = 0;
        try (BufferedReader r = Files.newBufferedReader(file)) { while (r.readLine() != null) lines++; }
        return lines;
    }

    /** (5) ALTERNATIVE: Files.readAllLines(): the whole file as a List<String> in memory, O(file size). */
    public static long readAllLines(Path file) throws IOException { return Files.readAllLines(file).size(); }
}
