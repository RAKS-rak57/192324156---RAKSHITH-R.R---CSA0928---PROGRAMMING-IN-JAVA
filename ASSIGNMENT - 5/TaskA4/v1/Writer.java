import java.io.*;
public class Writer { public static void main(String[] a) throws Exception {
    try (ObjectOutputStream o = new ObjectOutputStream(new FileOutputStream("fixed_v1.ser"))) { o.writeObject(new ResidentFixed("Arun", "192311001")); }
    try (ObjectOutputStream o = new ObjectOutputStream(new FileOutputStream("implicit_v1.ser"))) { o.writeObject(new ResidentImplicit("Arun", "192311001")); }
    System.out.println("version 1 (no phone field): wrote fixed_v1.ser and implicit_v1.ser");
    System.out.println("computed serialVersionUID of ResidentImplicit v1 = " + java.io.ObjectStreamClass.lookup(ResidentImplicit.class).getSerialVersionUID());
} }
