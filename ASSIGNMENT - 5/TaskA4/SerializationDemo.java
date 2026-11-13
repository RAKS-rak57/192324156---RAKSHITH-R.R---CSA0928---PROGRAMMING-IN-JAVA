import java.io.*;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.*;

/** Task A4.1: what is written when a Hostel is serialized, what transient does, and what happens with a live Connection. */
public class SerializationDemo {
    static Connection fakeLiveConnection() {                              // an object that implements Connection but is NOT Serializable
        return (Connection) Proxy.newProxyInstance(SerializationDemo.class.getClassLoader(), new Class<?>[]{Connection.class}, (p, m, a) -> null);
    }
    static int count;
    public static void main(String[] args) throws Exception {
        Connection live = fakeLiveConnection();
        Hostel h = new Hostel("Block A");
        Room r101 = new Room(101), r102 = new Room(102);
        Resident arun = new Resident("Arun", "192311001", "secret123", live);
        Resident divya = new Resident("Divya", "192311002", "pass456", live);
        r101.residents.add(arun); r101.residents.add(divya);
        r102.residents.add(arun);                                          // the SAME Resident object is also in room 102
        h.rooms.add(r101); h.rooms.add(r102);

        System.out.println("=== 1. Serialize the Hostel (one writeObject call) ===");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) { out.writeObject(h); }
        byte[] bytes = bos.toByteArray();
        System.out.println("bytes written: " + bytes.length);
        Hostel back;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) { back = (Hostel) in.readObject(); }
        System.out.println("hostel name after reading : " + back.name + ", rooms = " + back.rooms.size());
        for (Room r : back.rooms) System.out.println("  room " + r.number + " -> " + r.residents);
        Resident a1 = back.rooms.get(0).residents.get(0), a2 = back.rooms.get(1).residents.get(0);
        System.out.println("transient fields after reading: password = " + a1.password + ", conn = " + a1.conn + "   (not stored, reset to null)");
        System.out.println("Arun in room 101 and Arun in room 102 are the same object after reading? " + (a1 == a2) + "   (a shared reference is written once and shared again)");
        System.out.println("-> writeObject(hostel) also wrote ALL reachable objects: 1 Hostel, " + back.rooms.size() + " Rooms and 2 distinct Residents (3 references).");
        System.out.println("password visible in the file bytes? " + new String(bytes, "ISO-8859-1").contains("secret123"));

        System.out.println();
        System.out.println("=== 2. The wrong design: a live Connection that is not transient ===");
        BadResident bad = new BadResident("Arun", "192311001", "secret123", live);
        try (ObjectOutputStream out = new ObjectOutputStream(new ByteArrayOutputStream())) { out.writeObject(bad); }
        catch (NotSerializableException e) { System.out.println("NotSerializableException: " + e.getMessage().replaceAll("\\$Proxy[0-9]+", "<proxy>")); }
        BadResident bad2 = new BadResident("Arun", "192311001", "secret123", null);
        ByteArrayOutputStream b2 = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(b2)) { out.writeObject(bad2); }
        System.out.println("with conn = null the same class is written, but the password IS in the file: " + new String(b2.toByteArray(), "ISO-8859-1").contains("secret123"));
    }
}
