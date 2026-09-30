package org.conrad.billingservice.client;

import org.conrad.billingservice.dto.ApartmentDto;
import org.conrad.billingservice.security.InternalTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class ResidentServiceClient {

    private final RestClient restClient;
    private final InternalTokenProvider tokenProvider;

    public ResidentServiceClient(RestClient.Builder builder, @Value("${resident-service.base-url}") String baseUrl,
                                 InternalTokenProvider tokenProvider) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.tokenProvider = tokenProvider;
    }

    public List<ApartmentDto> getAllApartments() {
        return restClient.get()
                .uri("/apartments")
                .header("Authorization", "Bearer" + tokenProvider.issueServiceToken())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
