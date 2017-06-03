package com.atexpose.util.httpresponse;

import com.atexpose.dispatcher.PropertiesDispatcher;
import com.google.common.base.Joiner;
import io.schinzel.basicutils.Checker;
import io.schinzel.basicutils.EmptyObjects;
import io.schinzel.basicutils.str.Str;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * The purpose of this class is to represent an header header.
 * <p>
 * Created by Schinzel on 2017-05-31.
 */
class HttpHeader {
    @Getter
    private final Str header;


    @Builder
    HttpHeader(HttpStatusCode httpStatusCode, Map<String, String> customResponseHeaders,
               ContentType contentType, int browserCacheMaxAgeInSeconds, int contentLength) {
        if(customResponseHeaders == null){
            customResponseHeaders = EmptyObjects.EMPTY_MAP;
        }
        header = Str.create()
                .a("HTTP/1.1 ").acrlf(httpStatusCode.getCode())
                .a("Server: ").acrlf(PropertiesDispatcher.RESP_HEADER_SERVER_NAME)
                .a("Content-Type: ").acrlf(contentType.getContentType())
                .a("Cache-Control: ").a("max-age=").acrlf(String.valueOf(browserCacheMaxAgeInSeconds))
                .a("Content-Length: ").acrlf(String.valueOf(contentLength))
                //If there are custom response headers
                .ifTrue(Checker.isNotEmpty(customResponseHeaders))
                //Add the custom response headers
                .acrlf(Joiner.on("\r\n").withKeyValueSeparator(": ").join(customResponseHeaders))
                .endIf()
                .acrlf();
    }


}