package com.idrsolutions;

import com.idrsolutions.service.FileService;
import com.idrsolutions.util.MimeMap;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import org.apache.http.HttpException;

import java.io.*;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    @FunctionName("HttpExample")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                route = "convert",
                methods = {HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS,
                dataType = "binary")
                HttpRequestMessage<Optional<byte[]>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        if (request.getBody().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("File must be attached to request").build();
        }

        byte[] body = request.getBody().get();

        String mimeType = request.getHeaders().get("content-type-actual");

        if (mimeType == null || mimeType.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body("Please provide the file's mime-type in the header with the key: Content-Type-Actual").build();
        }

        if (!MimeMap.checkOfficeMimeType(mimeType)) {
            return request.createResponseBuilder(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body("Content-Type-Actual must be a valid office type").build();
        }

        String path = System.getenv("pdf:GraphEndpoint") + "sites/" + System.getenv("pdf:SiteId") + "/drive/items/";
        String fileId = null;

        try (InputStream stream = new ByteArrayInputStream(body)) {
            fileId = FileService.uploadStream(path, stream, body.length, mimeType);

            byte[] pdf = FileService.downloadConvertedFile(path, fileId, "pdf");

            return request.createResponseBuilder(HttpStatus.OK).body(pdf).build();
        } catch (IOException e) {
            e.printStackTrace();
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (HttpException e) {
            context.getLogger().warning(e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage()).build();
        } finally {
            if (fileId != null) {
                try {
                    FileService.deleteFile(path, fileId);
                } catch (HttpException | IOException e) {
                    context.getLogger().warning(e.getMessage());
                }
            }
        }
    }
}
