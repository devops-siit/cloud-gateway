package com.dislinkt.cloudgateway.config;

import com.dislinkt.cloudgateway.contracts.AccountDTO;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    private final WebClient.Builder webClientBuilder;

    public AuthFilter(WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                throw new RuntimeException("Missing authorization information");
            }

            String authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);

            String[] parts = authHeader.split(" ");

            if (parts.length != 2 || !"Bearer".equals(parts[0])) {
                throw new RuntimeException("Incorrect authorization structure");
            }

            return webClientBuilder.build()
                    .get()
                    .uri("http://localhost:8181/validate-token?token=" + parts[1])
                    .retrieve().bodyToMono(AccountDTO.class)
                    .map(accountDTO -> {
                        exchange.getRequest()
                                .mutate()
                                .header("X-auth-user-id", accountDTO.getUsername());
                        return exchange;
                    }).flatMap(chain::filter);
        };
    }

    public static class Config {
        // empty class as I don't need any particular configuration
    }
}