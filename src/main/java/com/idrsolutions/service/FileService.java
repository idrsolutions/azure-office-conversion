package com.idrsolutions.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.UUID;

public class FileService {
    private static String auth;

    private static Header createAuthorisedHeader() {
        if (auth == null) {
            auth = AuthService.getAccessToken();
        }

        return new BasicHeader("Authorization", "Bearer " + auth);
    }

    public static String uploadStream(String path, InputStream content, String contentType) {
        HttpClient client = HttpClients.createDefault();

        String fileName = UUID.randomUUID().toString() + ".tmp";

        HttpPut put = new HttpPut(path + "root:/" + fileName + ":/content");
        put.addHeader(createAuthorisedHeader());

        put.setEntity(new InputStreamEntity(content));
        put.setHeader("Content-type", contentType);

        try {
            HttpResponse response = client.execute(put);

            if (response.getStatusLine().getStatusCode() == 200) {
                Reader reader = new InputStreamReader(response.getEntity().getContent());
                Gson gson = new Gson();
                JsonObject json = gson.fromJson(reader, JsonObject.class);

                JsonElement token = json.get("id");

                return token != null ? token.getAsString() : null;
            } else {
                throw new HttpException("Failed to upload file" + response.getStatusLine().getReasonPhrase());
            }
         } catch (IOException | HttpException e) {
            e.printStackTrace();
        }
        return null;
    }
}
