package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CrptApi {

    private final String baseUrl = "https://ismp.crpt.ru/api/v3"; // URL API

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

    public void createDocument(ObjectNode document, String signature) throws Exception {
        String token = getAssesToken();
        checkRequestLimit();
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("product_document", document);
        requestBody.put("document_format", "MANUAL");
        requestBody.put("type", "LP_INTRODUCE_GOODS");
        requestBody.put("signature", signature);
        String requestJson = objectMapper.writeValueAsString(requestBody);
        RequestBody requestBodyObject = RequestBody.create(MediaType.parse("application/json"), requestJson);
        Request request = new Request.Builder()
                .url(baseUrl + "/lk/documents/create?pg=milk")
                .header("Authorization", "Bearer " + token)
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

    private String getAssesToken() throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + "/auth/cert/key")
                .build();
        Response response = client.newCall(request).execute();
        System.out.println(response);
        String responceBodyString = response.body().string();
        JsonNode responseJson = objectMapper.readTree(responceBodyString);
        String uuid = responseJson.get("uuid").asText();
        String data = responseJson.get("data").asText();

        JSONObject requestJsonBody = new JSONObject();
        requestJsonBody.put("uuid", uuid);
        requestJsonBody.put("data", data);
        RequestBody requestBodyObject = RequestBody.create(MediaType.parse("application/json"), requestJsonBody.toJSONString());
        request = new Request.Builder()
                .url(baseUrl + "/auth/cert/")
                .header("content-type", "application/json;charset=UTF-8")
                .post(requestBodyObject)
                .build();
        response = client.newCall(request).execute();
        responceBodyString = response.body().string();
        responseJson = objectMapper.readTree(responceBodyString);
        String tocken = responseJson.get("token").asText();
        return tocken;
    }
}
