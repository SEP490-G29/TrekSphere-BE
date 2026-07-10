package com.sep.treksphere.config;

import com.sendgrid.Client;
import com.sendgrid.SendGrid;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Configuration
public class EmailConfig {

    @Value("${sendgrid.api-key}")
    private String sendGridApiKey;

    @Bean
    public SendGrid sendGrid() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(10000)
                .setSocketTimeout(10000)
                .setConnectionRequestTimeout(10000)
                .build();

        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionTimeToLive(30, TimeUnit.SECONDS)
                .evictIdleConnections(15L, TimeUnit.SECONDS)
                .setRetryHandler(new DefaultHttpRequestRetryHandler(2, true) {
                    @Override
                    public boolean retryRequest(IOException exception, int executionCount,
                                                org.apache.http.protocol.HttpContext context) {
                        if (exception instanceof NoHttpResponseException) {
                            return executionCount <= 2;
                        }
                        return super.retryRequest(exception, executionCount, context);
                    }
                })
                .build();

        Client client = new Client(httpClient);
        return new SendGrid(sendGridApiKey, client);
    }
}
