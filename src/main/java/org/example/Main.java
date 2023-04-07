package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


public class Main {
    public static void main(String[] args) {

        CrptApi crptApi = new CrptApi(TimeUnit.MINUTES, 5);

//        String filePath = args [0];
//        String signature = args[1];

        String filePath = "./testFile.txt";
        String signature = "signature";

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            String file = sb.toString();
            ObjectNode doc = (ObjectNode) new ObjectMapper().readTree(file);

            crptApi.createDocument(doc, signature);


        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }
}