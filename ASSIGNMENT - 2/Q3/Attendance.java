import java.util.HashSet;
import java.util.Set;
import java.util.Objects;

// Q3 - Complete the Code, STEP 2: TODO 1 completed.
class Student {
    private final String regNo;
    private final String name;

    Student(String regNo, String name) {
        this.regNo = regNo;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Student)) return false;
        return regNo.equals(((Student) o).regNo);
    }

    // TODO 1 (completed): hashCode uses ONLY regNo, the same field used by equals()
    @Override
    public int hashCode() {
        return Objects.hash(regNo);
    }

    @Override
    public String toString() {
        return regNo + " " + name;
    }
}

public class Attendance {
    public static void main(String[] args) {
        Set<Student> present = new HashSet<>();
        present.add(new Student("192311001", "Arun"));   // scanned at Gate 1
        present.add(new Student("192311002", "Divya"));
        present.add(new Student("192311001", "Arun"));   // scanned again at Gate 3
        System.out.println("Unique students: " + present.size());
    }
}
