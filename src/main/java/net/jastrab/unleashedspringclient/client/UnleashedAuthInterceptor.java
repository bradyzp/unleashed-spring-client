package net.jastrab.unleashedspringclient.client;

import net.jastrab.unleashed.api.http.UnleashedConstants;
import net.jastrab.unleashed.api.security.ApiCredential;
import net.jastrab.unleashed.api.security.SignatureGenerator;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class UnleashedAuthInterceptor implements ClientHttpRequestInterceptor {

    private final ApiCredential credential;

    public UnleashedAuthInterceptor(ApiCredential credential) {
        Objects.requireNonNull(credential, "ApiCredential cannot be null");
        this.credential = credential;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().putIfAbsent(UnleashedConstants.ApiAuthId, List.of(credential.getId()));
        request.getHeaders().putIfAbsent(UnleashedConstants.ApiAuthSignature, List.of(generateSignature(request.getURI())));

        return execution.execute(request, body);
    }

    private String generateSignature(URI requestUri) {
        final Optional<String> queryString = Optional.ofNullable(requestUri.getQuery());
        return SignatureGenerator.getSignature(credential.getKey(), queryString.orElse(""));
    }
}
