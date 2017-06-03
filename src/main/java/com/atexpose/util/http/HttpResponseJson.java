package com.atexpose.util.http;

import io.schinzel.basicutils.UTF8;
import lombok.Builder;
import lombok.Getter;
import org.json.JSONObject;

import java.util.Map;

/**
 * Created by schinzel on 2017-06-03.
 */
public class HttpResponseJson {
    @Getter
    private final String response;

    @Builder
    HttpResponseJson(JSONObject body, Map<String, String> customResponseHeaders) {
        String bodyAsString = body.toString();
        int contentLength = UTF8.getBytes(bodyAsString).length;
        HttpHeader header = HttpHeader.builder()
                .httpStatusCode(HttpStatusCode.OK)
                .customResponseHeaders(customResponseHeaders)
                .contentType(ContentType.JSON)
                .contentLength(contentLength)
                .build();
        response = header.getHeader()
                .a(bodyAsString)
                .getString();
    }
}
