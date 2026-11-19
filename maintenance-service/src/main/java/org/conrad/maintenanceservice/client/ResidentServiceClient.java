package org.conrad.maintenanceservice.client;

import org.conrad.maintenanceservice.dto.ResidentDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;

@Component
public class ResidentServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ResidentServiceClient.class);

    private final RestClient restClient;

    public ResidentServiceClient(RestClient.Builder builder, @Value("${resident-service.base-url}") String baseUrl) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(2));
        this.restClient = builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    public Optional<ResidentDto> getCurrentResident(String authorizationHeader) {
        try {
            return Optional.ofNullable(restClient.get()
                    .uri("/auth/me")
                    .header("Authorization", authorizationHeader)
                    .retrieve()
                    .body(ResidentDto.class));
        } catch (RestClientException e) {
            log.warn("Could not look up the resident in resident-service: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
