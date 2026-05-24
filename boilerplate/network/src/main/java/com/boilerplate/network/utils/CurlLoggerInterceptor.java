package com.boilerplate.network.utils;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.nio.charset.Charset;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import timber.log.Timber;

public class CurlLoggerInterceptor implements Interceptor {
    private StringBuilder curlCommandBuilder;
    private final Charset UTF8 = Charset.forName("UTF-8");
    private String tag = null;

    public CurlLoggerInterceptor(String tag) {
        this.tag = tag;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        curlCommandBuilder = new StringBuilder("");
        // add cURL command
        curlCommandBuilder.append("cURL ");
        curlCommandBuilder.append("-X ");
        // add method
        curlCommandBuilder.append(request.method().toUpperCase() + " ");
        // adding headers
        for (String headerName : request.headers().names()) {
            addHeader(headerName, request.headers().get(headerName));
        }

        // adding request body
        RequestBody requestBody = request.body();
        if (request.body() != null) {
            Buffer buffer = new Buffer();
            requestBody.writeTo(buffer);
            Charset charset = UTF8;
            MediaType contentType = requestBody.contentType();
            if (contentType != null) {
                addHeader("Content-Type", request.body().contentType().toString());
                charset = contentType.charset(UTF8);
                curlCommandBuilder.append(" -d '" + buffer.readString(charset) + "'");
            }
        }

        // add request URL
        try {
            curlCommandBuilder.append(" \"" + request.url().toString() + "\"");
            curlCommandBuilder.append(" -L");
        } catch (Exception e) {
        }

        CurlPrinter.print(tag, request.url().toString(), curlCommandBuilder.toString());
        return chain.proceed(request);
    }

    private void addHeader(String headerName, String headerValue) {
        try {
            curlCommandBuilder.append("-H " + "\"" + headerName + ": " + headerValue + "\" ");
        } catch (Exception ignored) {
        }
    }
}

class CurlPrinter {

    private static final String SINGLE_DIVIDER = "───────────────────────────────CURL───────────────────────────────";

    private static String sTag = "CURL";

    public static void print(@Nullable String tag, String url, String msg) {
        // setting tag if not null
        if (tag != null)
            sTag = tag;

        StringBuilder logMsg = new StringBuilder("\n");
        logMsg.append("\n");
        logMsg.append("URL: " + url);
        logMsg.append("\n");
        logMsg.append(SINGLE_DIVIDER);
        logMsg.append("\n");
        logMsg.append(msg);
        logMsg.append(" ");
        logMsg.append(" \n");
        logMsg.append(SINGLE_DIVIDER);
        logMsg.append(" \n ");
        log(logMsg.toString());
    }

    private static void log(String msg) {
        Timber.d(sTag + " " + msg);
    }
}