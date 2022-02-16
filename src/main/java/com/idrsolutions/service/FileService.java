package com.idrsolutions.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.idrsolutions.util.MimeMap;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.UUID;

public class FileService {
    private static String auth;

    private static Header createAuthorisedHeader() {
        if (auth == null) {
            auth = AuthService.getAccessToken();
        }

        return new BasicHeader("Authorization", "Bearer " + auth);
    }

    public static String uploadStream(String path, InputStream content, long contentLength, String contentType) throws HttpException, IOException {
        HttpClient client = HttpClients.createDefault();

        String fileName = UUID.randomUUID() + "." + MimeMap.getExtension(contentType);

        HttpPut put = new HttpPut(path + "root:/" + fileName + ":/content");
        put.addHeader(createAuthorisedHeader());

        InputStreamEntity entity = new InputStreamEntity(content, contentLength, ContentType.create(contentType));
        entity.setChunked(true);
        put.setEntity(entity);

        HttpResponse response = client.execute(put);

        if (response.getStatusLine().getStatusCode() < 300) {
            Reader reader = new InputStreamReader(response.getEntity().getContent());
            Gson gson = new Gson();
            JsonObject json = gson.fromJson(reader, JsonObject.class);

            JsonElement token = json.get("id");

            if (token == null) throw new HttpException("Failed to upload file to sharepoint: response did not contain file ID");

            return token.getAsString();
        } else {
            throw new HttpException("Failed to upload file to sharepoint: " + response.getStatusLine().getStatusCode() + " - " + response.getStatusLine().getReasonPhrase());
        }
    }

    public static byte[] downloadConvertedFile(String path, String fileId, String targetFormat) throws IOException, HttpException {
        HttpClient client = HttpClients.createDefault();

        HttpGet get = new HttpGet(path + fileId + "/content?format=" + targetFormat);
        get.addHeader(createAuthorisedHeader());

        HttpResponse response = client.execute(get);

        if (response.getStatusLine().getStatusCode() < 300) {
            return response.getEntity().getContent().readAllBytes();
        } else {
            throw new HttpException("Failed to fetch converted file: " + response.getStatusLine().getStatusCode() + " - " + response.getStatusLine().getReasonPhrase());
        }
    }

    public static void deleteFile(String path, String fileId) throws HttpException, IOException {
        HttpClient client = HttpClients.createDefault();

        HttpDelete delete = new HttpDelete(path + fileId);
        delete.addHeader(createAuthorisedHeader());

        HttpResponse response = client.execute(delete);
        if (response.getStatusLine().getStatusCode() >= 300) {
            throw new HttpException("Failed to delete file: " + response.getStatusLine().getStatusCode() + " - " + response.getStatusLine().getReasonPhrase());
        }
    }
}
