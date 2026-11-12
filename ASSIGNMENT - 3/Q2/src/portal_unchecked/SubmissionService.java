package portal_unchecked;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
/** SERVICE layer: business rules. Declares (throws) what it cannot handle and lets it propagate upwards. */
public class SubmissionService {
    static final long SIZE_LIMIT_KB = 200;
    private final Map<String, Set<String>> registrations;     // course -> registered students
    private final Map<String, LocalDateTime> windowCloses;    // course -> deadline
    private final Storage storage;
    private final boolean lossy;

    public SubmissionService(Map<String, Set<String>> registrations, Map<String, LocalDateTime> windowCloses, Storage storage, boolean lossy) {
        this.registrations = registrations; this.windowCloses = windowCloses; this.storage = storage; this.lossy = lossy;
    }
    public void submit(String student, String course, String fileName, long sizeKb, LocalDateTime now) {
        if (student == null || student.isBlank() || fileName == null || fileName.isBlank() || sizeKb < 0)
            throw new InvalidSubmissionException("student, file name and a non-negative size are required");   // unchecked
        if (!registrations.getOrDefault(course, Set.of()).contains(student))
            throw new NotRegisteredException(student, course);
        LocalDateTime closes = windowCloses.get(course);
        if (closes == null || now.isAfter(closes))
            throw new ExamWindowClosedException(closes, now);
        if (sizeKb > SIZE_LIMIT_KB)
            throw new FileTooLargeException(fileName, sizeKb, SIZE_LIMIT_KB);
        if (lossy) storage.saveLossy(fileName, sizeKb); else storage.save(fileName, sizeKb);   // SubmissionStorageException propagates
    }
}
