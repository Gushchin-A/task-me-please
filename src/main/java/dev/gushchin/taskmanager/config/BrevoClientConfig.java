package dev.gushchin.taskmanager.config;

import java.net.http.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class BrevoClientConfig {
    @Bean
    public HttpClient brevoHttpClient(AppProperties appProperties) {
        return HttpClient.newBuilder()
                .connectTimeout(appProperties.getBrevo().getConnectTimeout())
                .build();
    }

    @Bean
    public RestClient brevoRestClient(
            RestClient.Builder builder, HttpClient brevoHttpClient, AppProperties appProperties) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(brevoHttpClient);
        requestFactory.setReadTimeout(appProperties.getBrevo().getReadTimeout());

        return builder.requestFactory(requestFactory).build();
    }
}
