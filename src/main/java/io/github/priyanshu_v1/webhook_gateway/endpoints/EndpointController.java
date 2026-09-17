package io.github.priyanshu_v1.webhook_gateway.endpoints;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.priyanshu_v1.webhook_gateway.auth.UserPrincipal;
import io.github.priyanshu_v1.webhook_gateway.endpoints.dto.EndpointCreateRequest;
import io.github.priyanshu_v1.webhook_gateway.endpoints.dto.EndpointResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/endpoints")
public class EndpointController {

    private final EndpointService endpointService;

    public EndpointController(EndpointService endpointService) {
        this.endpointService = endpointService;
    }

    @PostMapping
    public ResponseEntity<EndpointResponse> createEndpoint(
    		@AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EndpointCreateRequest request
    ) {
    	EndpointResponse response = endpointService.createEndpoint(principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<EndpointResponse>> getEndpoints(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<EndpointResponse> endpoints = endpointService.getEndpointsByUser(principal.userId());
        return ResponseEntity.ok(endpoints);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEndpoint(
    		 @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        endpointService.deleteEndpoint(id, principal.userId());
        return ResponseEntity.noContent().build();
    }
}