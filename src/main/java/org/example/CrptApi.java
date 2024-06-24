package org.example;

import com.google.gson.Gson;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CrptApi {
    private final Semaphore semaphore;
    private final AtomicInteger requestCount;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        semaphore = new Semaphore(requestLimit);
        requestCount = new AtomicInteger(0);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        scheduler.scheduleAtFixedRate(() -> {
            int availablePermits = semaphore.availablePermits();
            semaphore.release(requestLimit - availablePermits);
            requestCount.set(0);
        }, 0, 1, timeUnit);
    }

    public static void main(String[] args) {
        try {
            CrptApi api = new CrptApi(TimeUnit.MINUTES, 5);
            Document doc = new Document();
            api.createDocument(doc, "signature");
        } catch (Exception ignored) {

        }
    }

    public void createDocument(Document document, String signature) throws InterruptedException, IOException {
        semaphore.acquire();
        try {
            OkHttpClient okHttpClient = new OkHttpClient();
            Gson gsonObject = new Gson();
            String json = gsonObject.toJson(document);
            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            RequestBody formBody = RequestBody.create(mediaType, json);
            Request request = new Request.Builder()
                    .url("https://ismp.crpt.ru/api/v3/lk/documents/create")
                    .header("Content-Type", "application/json")
                    .header("Signature", signature)
                    .post(formBody)
                    .build();
            Response response = okHttpClient.newCall(request).execute();
            if (response.code() != 200) {
                throw new IOException("Failed to create document: " + response.body());
            }
        } finally {
            semaphore.release();
        }
    }

    public static class Document {
        public Description description;
        public String doc_id;
        public String doc_status;
        public String doc_type = "LP_INTRODUCE_GOODS";
        public boolean importRequest;
        public String owner_inn;
        public String participant_inn;
        public String producer_inn;
        public String production_date;
        public String production_type;
        public Product[] products;
        public String reg_date;
        public String reg_number;
    }
    public static class Description {
        public String participantInn;
    }
    public static class Product {
        public String certificate_document;
        public String certificate_document_date;
        public String certificate_document_number;
        public String owner_inn;
        public String producer_inn;
        public String production_date;
        public String tnved_code;
        public String uit_code;
        public String uitu_code;
    }
}