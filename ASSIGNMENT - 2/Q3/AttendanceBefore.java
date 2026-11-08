import java.util.HashSet;
import java.util.Set;

// Q3 - Complete the Code, STEP 1: TODO 1 is NOT completed (hashCode is not overridden).
class StudentBefore {
    private final String regNo;
    private final String name;

    StudentBefore(String regNo, String name) {
        this.regNo = regNo;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StudentBefore)) return false;
        return regNo.equals(((StudentBefore) o).regNo);
    }

    // TODO 1: hashCode() is deliberately NOT overridden in this "before" version

    @Override
    public String toString() {
        return regNo + " " + name;
    }
}

public class AttendanceBefore {
    public static void main(String[] args) {
        Set<StudentBefore> present = new HashSet<>();
        present.add(new StudentBefore("192311001", "Arun"));   // scanned at Gate 1
        present.add(new StudentBefore("192311002", "Divya"));
        present.add(new StudentBefore("192311001", "Arun"));   // scanned again at Gate 3
        System.out.println("Unique students: " + present.size());
    }
}
