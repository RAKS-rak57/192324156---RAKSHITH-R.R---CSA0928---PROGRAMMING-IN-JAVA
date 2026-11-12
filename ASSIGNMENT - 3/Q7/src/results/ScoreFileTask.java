package results;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;

/** One unit of work: read, validate and score ONE file. It converts every expected failure into a FileResult. */
public class ScoreFileTask implements Callable<FileResult> {
    private final Path file; private final ConcurrentMap<String, FileResult> sink; private final long latencyMicros;

    public ScoreFileTask(Path file, ConcurrentMap<String, FileResult> sink, long latencyMicros) {
        this.file = file; this.sink = sink; this.latencyMicros = latencyMicros;
    }

    @Override public FileResult call() {
        String name = file.getFileName().toString();
        FileResult r;
        try {
            if (latencyMicros > 0) Thread.sleep(latencyMicros / 1000, (int) (latencyMicros % 1000) * 1000);   // SIMULATED storage latency
            String content = Files.readString(file);                       // NoSuchFileException if missing
            ScoreParser.Parsed p = ScoreParser.parse(content);
            r = FileResult.ok(name, p.id(), p.total(), p.average(), p.grade());
        } catch (NoSuchFileException e) {
            r = FileResult.failed(name, Status.MISSING, "file not found");
        } catch (CorruptFileException e) {
            r = FileResult.failed(name, Status.CORRUPT, e.getMessage());
        } catch (java.nio.charset.CharacterCodingException e) {            // FIX after the hand-checked test: bytes that are not valid text
            r = FileResult.failed(name, Status.CORRUPT, "not valid UTF-8 text");
        } catch (IOException e) {
            r = FileResult.failed(name, Status.ERROR, "I/O error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();                            // keep the interrupt flag for the pool
            r = FileResult.failed(name, Status.ERROR, "interrupted");
        } catch (RuntimeException e) {                                     // a bug must not stop the run either
            r = FileResult.failed(name, Status.ERROR, "unexpected " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        sink.put(name, r);                                                 // shared result map: many threads write concurrently
        return r;
    }
}
