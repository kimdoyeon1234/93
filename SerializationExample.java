import java.io.*;

public class SerializationExample {

    public static void saveData(String filePath, DecorTime decorTime) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(decorTime);
            System.out.println("Data saved successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static DecorTime loadData(String filePath) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            return (DecorTime) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
