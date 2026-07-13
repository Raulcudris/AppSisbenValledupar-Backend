package com.appsisben.backend.shared.export;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

public final class ExcelResponseUtil {

    private static final String EXCEL_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private ExcelResponseUtil() {
    }

    public static ResponseEntity<ByteArrayResource> asAttachment(byte[] bytes, String filename) {
        ByteArrayResource resource = new ByteArrayResource(bytes);

        ContentDisposition contentDisposition = ContentDisposition
                .attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(EXCEL_CONTENT_TYPE))
                .contentLength(bytes.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(resource);
    }
}
