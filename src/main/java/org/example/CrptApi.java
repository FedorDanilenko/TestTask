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
        // Получение токена для доступа к ЧЗ
        String token = getAssesToken();

        // Проверка времени запроса
        checkRequestLimit();

        // Создание тела запроса
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("product_document", document);
        requestBody.put("document_format", "MANUAL");
        requestBody.put("type", "LP_INTRODUCE_GOODS");
        requestBody.put("signature", signature);
        String requestJson = objectMapper.writeValueAsString(requestBody);
        RequestBody requestBodyObject = RequestBody.create(MediaType.parse("application/json"), requestJson);

        //Создание запроса
        Request request = new Request.Builder()
                .url(baseUrl + "/lk/documents/commissioning/contract/create") // url куда оправляется запрос
                .header("Authorization", "Bearer " + token) // передача полученно токена
                .post(requestBodyObject) // добавление тела запроса
                .build();
        Response response = client.newCall(request).execute(); // получение ответа
        // ошибка если ответ не был получен
        if (!response.isSuccessful()) throw new Exception("HTTP error code: " + response.code());
    }

    private void checkRequestLimit() throws InterruptedException {
        lock.lock();
        try {
            long currentTimeLimit = System.currentTimeMillis(); // текущее время
            long timeSinceLastRequestMils = currentTimeLimit - lastRequestTimeMillis; // время с момента последнего запроса
            // проверка что время между запросами не превышает устанновыленный лимит
            if (timeSinceLastRequestMils >= requestIntervalMillis) {
                lastRequestTimeMillis = currentTimeLimit;
                requestCount = 1; // обнудение счетчика запросов
            } else {
                requestCount++;
                // проверка что количество запросов не превыщает установленный лимит
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

    private String getAssesToken() throws Exception {
        String token = "";

        // запрос для получения uuid и data
        Request request = new Request.Builder()
                .url(baseUrl + "/auth/cert/key")
                .get()
                .build();
        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
            String responceBodyString = response.body().string();
            JsonNode responseJson = objectMapper.readTree(responceBodyString);
            // Сохраняем uuid и data если ответ был успешно получен
            String uuid = responseJson.get("uuid").asText();
            String data = responseJson.get("data").asText();

            // запрос для получения токена доступа
            // помещяем ранее полученные uuid и data в тело зароса
            JSONObject requestJsonBody = new JSONObject();
            requestJsonBody.put("uuid", uuid);
            requestJsonBody.put("data", data);
            RequestBody requestBodyObject = RequestBody.create(MediaType.parse("application/json"), requestJsonBody.toJSONString());
            request = new Request.Builder()
                    .url(baseUrl + "/auth/cert")
                    .header("content-type", "application/json;charset=UTF-8")
                    .post(requestBodyObject)
                    .build();
            response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                // сохраняем токен если ответ был успешный
                responceBodyString = response.body().string();
                responseJson = objectMapper.readTree(responceBodyString);
                token = responseJson.get("token").asText();
                return token;
            } else {
                throw new Exception("HTTP error code: " + response.code());
            }
        } else throw new Exception("HTTP error code: " + response.code());
    }
}
