import java.io.*;
public class Reader { public static void main(String[] a) throws Exception {
    System.out.println("version 2 (class now has a phone field) reads the OLD files:");
    System.out.println("computed serialVersionUID of ResidentImplicit v2 = " + java.io.ObjectStreamClass.lookup(ResidentImplicit.class).getSerialVersionUID());
    try (ObjectInputStream i = new ObjectInputStream(new FileInputStream("fixed_v1.ser"))) { ResidentFixed r = (ResidentFixed) i.readObject();
        System.out.println("  explicit serialVersionUID = 1L : OK  -> name=" + r.name + ", regNo=" + r.regNo + ", phone=" + r.phone + "   (new field gets its default value)"); }
    try (ObjectInputStream i = new ObjectInputStream(new FileInputStream("implicit_v1.ser"))) { ResidentImplicit r = (ResidentImplicit) i.readObject(); System.out.println("  no serialVersionUID           : OK  -> " + r.name); }
    catch (InvalidClassException e) { System.out.println("  no serialVersionUID           : InvalidClassException -> " + e.getMessage()); }
} }
