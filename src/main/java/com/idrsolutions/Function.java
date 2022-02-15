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
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<byte[]>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        // TODO: Check valid request
        // TODO: Get file and length and mimetype
        // TODO: Return conversion result

        try {
            String mimeType = MimeMap.getMimeType(filePath.toString().substring(filePath.toString().lastIndexOf(".") + 1));

            String path = System.getenv("pdf:GraphEndpoint") + "sites/" + System.getenv("pdf:SiteId") + "/drive/items/";
            String fileId = FileService.uploadStream(path, stream, filePath.toFile().length(), mimeType);

            if (fileId == null) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Failed to upload file to sharepoint").build();
            }

            byte[] pdf = FileService.downloadConvertedFile(path, fileId, "pdf");
            FileService.deleteFile(path, fileId);
            if (pdf == null) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Failed to convert file").build();
            }

            return request.createResponseBuilder(HttpStatus.OK).

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
