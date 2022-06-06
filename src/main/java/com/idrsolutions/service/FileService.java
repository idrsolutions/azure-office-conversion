package com.idrsolutions.service;

import com.azure.identity.ClientSecretCredentialBuilder;
import com.idrsolutions.util.MimeMap;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.DriveItemCreateUploadSessionParameterSet;
import com.microsoft.graph.models.DriveItemUploadableProperties;
import com.microsoft.graph.models.UploadSession;
import com.microsoft.graph.options.QueryOption;
import com.microsoft.graph.requests.DriveItemRequestBuilder;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.tasks.IProgressCallback;
import com.microsoft.graph.tasks.LargeFileUploadResult;
import com.microsoft.graph.tasks.LargeFileUploadTask;
import okhttp3.Request;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class FileService {
    private static final GraphServiceClient<Request> graphClient = GraphServiceClient
            .builder()
            .authenticationProvider(
                    new TokenCredentialAuthProvider(
                            new ClientSecretCredentialBuilder()
                                    .clientId(System.getenv("graph:ClientId"))
                                    .clientSecret(System.getenv("graph:ClientSecret"))
                                    .tenantId(System.getenv("graph:TenantId"))
                                    .build()
                    )
            )
            .buildClient();

    /**
     * Get a DriveItemRequestBuilder already at the root of the configured sharepoint site
     * @return A DriveItemRequestBuilder at the site root
     */
    private static DriveItemRequestBuilder getSharepointSiteRoot() {
        return graphClient
                .sites(System.getenv("pdf:SiteId"))
                .drive()
                .root();
    }

    /**
     * Uploads the given file to the given sharepoint storage
     * @param content An input stream of the file being uploaded
     * @param contentLength The length in bytes of the file being uploaded
     * @param contentType The Mimetype of the file being uploaded
     * @return The id of the file in the sharepoint storage
     * @throws IOException when the upload fails
     * @throws ClientException when the post request fails
     */
    public static String uploadStream(ExecutionContext context, InputStream content, long contentLength, String contentType) throws ClientException, IOException {
        String fileName = UUID.randomUUID() + "." + MimeMap.getExtension(contentType);

        IProgressCallback callback = (current, max) ->
                context.getLogger().info(String.format("Uploaded %d of %d bytes", current, max));

        DriveItemCreateUploadSessionParameterSet uploadParams = DriveItemCreateUploadSessionParameterSet
                .newBuilder()
                .withItem(new DriveItemUploadableProperties())
                .build();

        UploadSession session = getSharepointSiteRoot()
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
     * @param fileId The ID of the file to download
     * @param targetFormat The target format to download the file in
     * @return a byte array containing the converted file
     * @throws ClientException when the graph api request failsw
     * @throws IOException when the converted file cannot be read from the response
     */
    public static byte[] downloadConvertedFile(String fileId, String targetFormat) throws IOException, ClientException {
        // It seems that the Java API is lacking a proper download function for items, we will need to make a custom request for the resource
        try (
                InputStream stream = graphClient
                    .customRequest("/sites/" + System.getenv("pdf:SiteId") + "/drive/items/" + fileId + "/content", InputStream.class)
                    .buildRequest(new QueryOption("format", targetFormat))
                    .get()
        ) {
            if (stream != null) {
                return stream.readAllBytes();
            }

            throw new IOException("Failed to read file from response");
        }
    }

    /**
     * Delete the file with the given fileId
     * @param fileId The ID of the file to delete
     * @throws ClientException when the graph api request fails
     */
    public static void deleteFile(String fileId) throws ClientException {
        DriveItem item = getSharepointSiteRoot()
                .itemWithPath(fileId)
                .buildRequest()
                .delete();

        System.out.println(item.deleted.state);
    }
}
