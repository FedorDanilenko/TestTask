package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;

import javax.swing.text.Document;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Main {
    public static void main(String[] args) {

        CrptApi crptApi = new CrptApi(TimeUnit.MINUTES, 5);

        String signature = args[1];
        try (FileReader fileReader = new FileReader(args[0])){

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static class CrptApi {

        private final String baseUrl = "https://example.com/api"; // здесь указать URL API

        private final OkHttpClient client;
        private final ObjectMapper objectMapper;
        private final Lock lock;
        private final int requestLimit;
        private final long requestIntervalMillis;
        private long lastRequestTimeMillis;
        private int requestCount;

        public CrptApi(TimeUnit timeUnit, int requestLimit) {
            this.client = new OkHttpClient();
            this.objectMapper = new ObjectMapper();
            this.lock = new ReentrantLock();
            this.requestLimit = requestLimit;
            this.requestIntervalMillis = timeUnit.toMillis(1);
            this.lastRequestTimeMillis = System.currentTimeMillis();
            this.requestCount = 0;
        }

        public void createDocument (ObjectNode document, String signature) throws Exception {
            checkRequestLimit();
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.set("document", document);
            requestBody.put("signature", signature);
            String requestJson = objectMapper.writeValueAsString(requestBody);
            RequestBody requestBodyObject = RequestBody.create(MediaType.parse("application/json"), requestJson);
            Request request = new Request.Builder()
                    .url(baseUrl + "/createDocument")
                    .post(requestBodyObject)
                    .build();
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) throw new Exception("HTTP error code: " + response.code());
        }

        private void checkRequestLimit() throws InterruptedException {
            lock.lock();
            try {
                long currentTimeLimit = System.currentTimeMillis();
                long timeSinceLastRequestMils = currentTimeLimit - lastRequestTimeMillis;
                if (timeSinceLastRequestMils >= requestIntervalMillis) {
                    lastRequestTimeMillis = currentTimeLimit;
                    requestCount = 1;
                } else {
                    requestCount++;
                    if (requestCount > requestLimit) {
                        long waitMillis = requestIntervalMillis - timeSinceLastRequestMils;
                        Thread.sleep(waitMillis);
                        lastRequestTimeMillis = System.currentTimeMillis();
                        requestCount = 1;
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }
}