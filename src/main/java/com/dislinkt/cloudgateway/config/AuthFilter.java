package com.dislinkt.cloudgateway.config;

import com.dislinkt.cloudgateway.contracts.AccountDTO;
import com.dislinkt.cloudgateway.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    private final WebClient.Builder webClientBuilder;

    @Value("${api.auth}")
    private String authApiPath;

    public AuthFilter(WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            try {
                if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    throw new UnauthorizedException("Missing authorization information");
                }

                String authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);

                String[] parts = authHeader.split(" ");

                if (parts.length != 2 || !"Bearer".equals(parts[0])) {
                    throw new UnauthorizedException("Incorrect authorization structure");
                }

                return webClientBuilder.build()
                        .get()
                        .uri(String.format("%s/validate-token?token=%s", authApiPath, parts[1]))
                        .retrieve()
                        .onStatus(HttpStatus::isError, response -> {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return Mono.empty();
                        })
                .bodyToMono(AccountDTO.class)
                        .map(accountDTO -> {
                            exchange.getRequest()
                                    .mutate()
                                    .header("X-auth-user-id", accountDTO.getUsername());
                            return exchange;
                        }).flatMap(chain::filter);
            } catch (UnauthorizedException ex) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        };
    }

    public static class Config {
        // empty class as I don't need any particular configuration
    }
}