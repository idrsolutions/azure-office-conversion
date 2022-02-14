package com.idrsolutions.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class AuthService {
    public static String getAccessToken() {
        List<NameValuePair> values = new ArrayList<>();
        values.add(new BasicNameValuePair("client_id", System.getenv("graph:ClientId")));
        values.add(new BasicNameValuePair("client_secret", System.getenv("graph:ClientSecret")));
        values.add(new BasicNameValuePair("scope", System.getenv("graph:Scope")));
        values.add(new BasicNameValuePair("grant_type", System.getenv("graph:GrantType")));
        values.add(new BasicNameValuePair("resource", System.getenv("graph:Resource")));

        String path = System.getenv("graph:EndPoint") + System.getenv("graph:TenantId") + "/oauth2/token";

        HttpClient client = HttpClients.createDefault();

        HttpPost post = new HttpPost(path);
        try {
            post.setEntity(new UrlEncodedFormEntity(values));
            HttpResponse response = client.execute(post);
            if (response.getStatusLine().getStatusCode() == 200) {
                Reader reader = new InputStreamReader(response.getEntity().getContent());
                Gson gson = new Gson();
                JsonObject json = gson.fromJson(reader, JsonObject.class);

                JsonElement token = json.get("access_token");

                return token != null ? token.getAsString() : null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
