package com.fashionstore.utils;

import java.io.*;

public class FileUtils {

    public static void writeObjectToFile(Object object, String filePath) throws IOException {
        File file = new File(filePath);
        File parent = file.getParentFile();

        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(filePath);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(object);
        }
    }

    public static Object readObjectFromFile(String filePath) throws IOException, ClassNotFoundException {
        File file = new File(filePath);

        if (!file.exists()) {
            return null;
        }

        try (FileInputStream fis = new FileInputStream(filePath);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            return ois.readObject();
        }
    }
}