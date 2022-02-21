package com.idrsolutions;

import com.idrsolutions.service.FileService;
import com.idrsolutions.util.MimeMap;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import org.apache.http.HttpException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    @FunctionName("Office2PDF")
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

        // In order for this to accept a raw file, the content type needs to be "application/octet-stream", however, we
        // still need rely on the content type of the original file, thus we need it delivered separately
        String mimeType = request.getHeaders().get("content-type-actual");

        if (mimeType == null || mimeType.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body("Please provide the file's mime-type in the header with the key: Content-Type-Actual").build();
        } else if (!MimeMap.checkOfficeMimeType(mimeType)) {
            return request.createResponseBuilder(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body("Content-Type-Actual must be a valid office type").build();
        }

        String path = System.getenv("pdf:GraphEndpoint") + "sites/" + System.getenv("pdf:SiteId") + "/drive/items/";
        String fileId = null;

        try (InputStream stream = new ByteArrayInputStream(body)) {
            fileId = FileService.uploadStream(path, stream, body.length, mimeType);
            byte[] pdf = FileService.downloadConvertedFile(path, fileId, "pdf");

            return request.createResponseBuilder(HttpStatus.OK).body(pdf).build();
        } catch (IOException e) {
            context.getLogger().warning(e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage()).build();
        } catch (HttpException e) {
            context.getLogger().warning(e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage()).build();
        } finally {
            // Since we can exit early during the conversion, we need to make sure that if a file was created, it gets
            // deleted, successful conversion or not
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
