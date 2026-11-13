public class Student {
    public String regNo, name, email;
    public int year;
    public Student(String regNo, String name, String email, int year) { this.regNo = regNo; this.name = name; this.email = email; this.year = year; }
    @Override public String toString() { return regNo + " " + name + " <" + email + "> year " + year; }
}
