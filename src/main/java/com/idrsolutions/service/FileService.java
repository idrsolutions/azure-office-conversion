package com.idrsolutions.service;

import com.azure.core.credential.TokenCredential;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.idrsolutions.util.MimeMap;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.graph.auth.confidentialClient.ClientCredentialProvider;
import com.microsoft.graph.auth.enums.NationalCloud;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.DriveItemCreateUploadSessionParameterSet;
import com.microsoft.graph.models.DriveItemUploadableProperties;
import com.microsoft.graph.models.UploadSession;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.tasks.IProgressCallback;
import com.microsoft.graph.tasks.LargeFileUploadResult;
import com.microsoft.graph.tasks.LargeFileUploadTask;
import okhttp3.Request;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class FileService {
    private static String auth;

    /**
     * Requests an access token from Azure to authorise future requests with
     */
    public static String getAccessToken() throws IOException, HttpException {
        List<NameValuePair> values = new ArrayList<>();
        values.add(new BasicNameValuePair("client_id", System.getenv("graph:ClientId")));
        values.add(new BasicNameValuePair("client_secret", System.getenv("graph:ClientSecret")));
        values.add(new BasicNameValuePair("scope", System.getenv("graph:Scope")));
        values.add(new BasicNameValuePair("grant_type", System.getenv("graph:GrantType")));
        values.add(new BasicNameValuePair("resource", System.getenv("graph:Resource")));

        String path = System.getenv("graph:EndPoint") + System.getenv("graph:TenantId") + "/oauth2/token";

        HttpClient client = HttpClients.createDefault();

        HttpPost post = new HttpPost(path);
        post.setEntity(new UrlEncodedFormEntity(values));
        HttpResponse response = client.execute(post);
        if (response.getStatusLine().getStatusCode() == 200) {
            Reader reader = new InputStreamReader(response.getEntity().getContent());
            Gson gson = new Gson();
            JsonObject json = gson.fromJson(reader, JsonObject.class);

            JsonElement token = json.get("access_token");

            return token != null ? token.getAsString() : null;
        } else {
            throw new HttpException("Failed to get access token: " + response.getStatusLine().getStatusCode() + " - " + response.getStatusLine().getReasonPhrase());
        }
    }

    /**
     * Creates a header that contains the authorisation required to make requests to our azure environment
     */
    private static Header createAuthorisedHeader() throws IOException, HttpException {
        if (auth == null) {
            auth = getAccessToken();
        }

        return new BasicHeader("Authorization", "Bearer " + auth);
    }

    /**
     * Uploads the given file to the given sharepoint storage
     * @param path The sharepoint address
     * @param content An input stream of the file being uploaded
     * @param contentLength The length in bytes of the file being uploaded
     * @param contentType The Mimetype of the file being uploaded
     * @return The id of the file in the sharepoint storage
     * @throws HttpException when an unexpected answer is received while making an HTTP Request
     * @throws IOException when an HTTP request fails
     */
    public static String uploadStream(ExecutionContext context, String path, InputStream content, long contentLength, String contentType) throws IOException {
        List<String> scopes = Arrays.asList(System.getenv("graph:Scope").split(","));

        GraphServiceClient<Request> graphClient = GraphServiceClient
                .builder()
                .authenticationProvider(
                    new ClientCredentialProvider(
                        System.getenv("graph:ClientId"),
                        scopes,
                        System.getenv("graph:ClientSecret"),
                        System.getenv("graph:TenantId"),
                        null
                    )
                )
                .buildClient();

        String fileName = UUID.randomUUID() + "." + MimeMap.getExtension(contentType);

        IProgressCallback callback = (current, max) ->
                context.getLogger().info(String.format("Uploaded %d of %d bytes", current, max));

        DriveItemCreateUploadSessionParameterSet uploadParams = DriveItemCreateUploadSessionParameterSet
                .newBuilder()
                .withItem(new DriveItemUploadableProperties())
                .build();

        UploadSession session = graphClient
                .sites(System.getenv("pdf:SiteId"))
                .drive()
                .root()
                .itemWithPath(fileName)
                .createUploadSession(uploadParams)
                .buildRequest()
                .post();

        LargeFileUploadTask<DriveItem> largeFileUploadTask = new LargeFileUploadTask<>(
                session,
                graphClient,
                content,
                contentLength,
                DriveItem.class);

        LargeFileUploadResult<DriveItem> result = largeFileUploadTask.upload(0, null, callback);

        if (result.responseBody != null) {
            return result.responseBody.id;
        }

        return null;
    }

    /**
     * Download the file with the given fileId in the targetFormat
     * @param path The sharepoint address
     * @param fileId The ID of the file to download
     * @param targetFormat The target format to download the file in
     * @return a byte array containing the converted file
     * @throws HttpException when an unexpected answer is received while making an HTTP Request
     * @throws IOException when an HTTP request fails or the converted file cannot be read from the response
     */
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

    /**
     * Delete the file with the given fileId
     * @param path The sharepoint address
     * @param fileId The ID of the file to delete
     * @throws HttpException when an unexpected answer is received while making an HTTP Request
     * @throws IOException when an HTTP request fails
     */
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
