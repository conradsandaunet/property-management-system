package org.conrad.billingservice.client;

import org.conrad.billingservice.dto.ApartmentDto;
import org.conrad.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
public class ResidentServiceClient {

    private static final String SERVICE_SUBJECT = "billing-service";

    private final RestClient restClient;
    private final JwtService jwtService;

    public ResidentServiceClient(RestClient.Builder builder, @Value("${resident-service.base-url}") String baseUrl,
                                 JwtService jwtService) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.jwtService = jwtService;
    }

    public List<ApartmentDto> getAllApartments() {
        try {
            List<ApartmentDto> apartments = restClient.get()
                    .uri("/apartments")
                    .header("Authorization", "Bearer " + jwtService.issueToken(SERVICE_SUBJECT, null, true))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return apartments != null ? apartments : List.of();
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Could not fetch apartments from resident-service", e);
        }
    }
}
