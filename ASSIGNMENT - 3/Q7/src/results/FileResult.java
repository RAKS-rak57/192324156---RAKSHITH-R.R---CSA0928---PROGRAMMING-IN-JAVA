package results;
/** Immutable result for one file - safe to hand from a worker thread to the main thread. */
public record FileResult(String fileName, Status status, String studentId, int total, double average, String grade, String message) {
    static FileResult ok(String file, String id, int total, double avg, String grade) { return new FileResult(file, Status.SUCCESS, id, total, avg, grade, ""); }
    static FileResult failed(String file, Status s, String msg) { return new FileResult(file, s, "", 0, 0, "", msg); }
}
