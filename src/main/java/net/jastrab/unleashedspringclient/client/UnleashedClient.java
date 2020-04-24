package net.jastrab.unleashedspringclient.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.jastrab.unleashed.api.CreateItemRequest;
import net.jastrab.unleashed.api.GetProductRequest;
import net.jastrab.unleashed.api.SimpleGetRequest;
import net.jastrab.unleashed.api.http.*;
import net.jastrab.unleashed.api.models.*;
import net.jastrab.unleashedspringclient.utils.ReflectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class UnleashedClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnleashedClient.class);
    private final String baseUri;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    public UnleashedClient(final String baseUri,
                           final RestTemplateBuilder builder,
                           final MappingJackson2HttpMessageConverter converter) {
        this.baseUri = baseUri;
        this.restTemplate = builder
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .messageConverters(converter)
                .build();

        this.mapper = converter.getObjectMapper();
        LOGGER.debug("UnleashedClient successfully initialized");
    }

    public <T> PaginatedUnleashedResponse<T> getItemsPaginated(PaginatedUnleashedRequest<T> request) {
        Objects.requireNonNull(request, "Request cannot be null");

        final Optional<PaginatedUnleashedResponse<T>> response = this.exchange(request);


        return response.orElseThrow();
    }

    /**
     * Request a list of items from Unleashed API. The response contains only the first page of items from a paginated
     * response.
     * <p>
     * If making a request for a large number of items (> 200) which may be paginated by unleashed,
     * use {@link #getItemsPaginated(PaginatedUnleashedRequest)}
     *
     * @param request The PaginatedUnleashedRequest for the item
     * @param <T>     type of the item that will be returned from the request
     * @return List<T> containing the items retrieved from the API, potentially empty
     */
    public <T> List<T> getItems(PaginatedUnleashedRequest<T> request) {
        Objects.requireNonNull(request, "Request cannot be null");
        LOGGER.debug("Performing getItems request, path: {}, query: {}, type <{}>",
                request.getPath(), request.getQuery(), request.getResponseType());

        final Optional<PaginatedUnleashedResponse<T>> response = this.exchange(request);

        return response.map(PaginatedUnleashedResponse::getItems).orElse(new ArrayList<>());
    }

    /**
     * Get a single item from a request. Useful utility for requests which are only expected to return a single result.
     * <p>
     * If more than one result is returned by the request, this method will return only the first
     *
     * @param request The PaginatedUnleashedRequest for the item
     * @param <T>     type of the item that will be returned by the request
     * @return Optional containing the first result from the request, or an empty optional if nothing was returned
     */
    public <T> Optional<T> getItem(PaginatedUnleashedRequest<T> request) {
        return this.getItems(request).stream().findFirst();
    }

    public <T extends CreatableResource> Optional<T> createItem(T item, Class<T> itemType) {
        LOGGER.debug("Creating item {} of type {}", item, itemType.getName());
        UnleashedRequest<T> request = new CreateItemRequest<>(item, itemType);

        return this.exchange(request);
    }

    /**
     * Create or Update a Product
     *
     * @return the newly created or updated Product from the origin (with real GUID from unleashed)
     * @since 0.5.8
     */
    public Optional<Product> upsertProduct(Product product, boolean update) {
        LOGGER.debug("Upserting product with code: {}", product.getProductCode());
        final PaginatedUnleashedRequest<Product> request = GetProductRequest.builder()
                .productCode(product.getProductCode())
                .build();

        return getItem(request).map(original -> {
            LOGGER.info("Retrieved existing product {}", original);
            if (update) {
                LOGGER.info("Updating existing product with properties from {}", product);
                ReflectUtils.updateEntity(original, product);
                LOGGER.info("Updated product: {}", original);
            }
            return createItem(original, Product.class);
        }).orElseGet(() -> createItem(product, Product.class));
    }

    @Cacheable("attribute_sets")
    public List<AttributeSet> getAttributeSets() {
        return getItems(new SimpleGetRequest<>(AttributeSet.class));
    }

    @Cacheable("payment_terms")
    public List<PaymentTerm> getPaymentTerms() {
        return getItems(new SimpleGetRequest<>(PaymentTerm.class));
    }

    @Cacheable("product_groups")
    public List<ProductGroup> getProductGroups() {
        return getItems(new SimpleGetRequest<>(ProductGroup.class));
    }

    @Cacheable("units_measure")
    public List<UnitOfMeasure> getUnitsOfMeasure() {
        return getItems(new SimpleGetRequest<>(UnitOfMeasure.class));
    }

    @Cacheable("customer_types")
    public List<CustomerType> getCustomerTypes() {
        return getItems(new SimpleGetRequest<>(CustomerType.class));
    }

    private <T, R> Optional<R> exchange(UnleashedRequest<T> request) {
        final HttpMethod method = HttpMethod.valueOf(request.getHttpMethod().name());
        LOGGER.debug("Request method: {}", method);
        final HttpHeaders headers = new HttpHeaders(new LinkedMultiValueMap<>(request.getHeaders()));
        LOGGER.debug("Request headers: {}", headers);

        final URI requestUri = UriComponentsBuilder
                .fromHttpUrl(this.baseUri)
                .path(request.getPath())
                .query(request.getQuery())
                .build()
                .toUri();
        LOGGER.debug("Request URI: {}", requestUri);

        try {
            final ResponseEntity<R> response = this.restTemplate.exchange(
                    requestUri,
                    method,
                    new HttpEntity<>(request.getRequestBody(), headers),
                    new ParameterizedTypeReference<>() {
                        @Override
                        public Type getType() {
                            return request.getResponseType();
                        }
                    }
            );
            LOGGER.debug("Response status code: {}", response.getStatusCode());
            return Optional.ofNullable(response.getBody());
        } catch (HttpClientErrorException e) {
            LOGGER.error("Request failed with status code: {}, body: {}", e.getStatusCode(), e.getResponseBodyAsString());

            UnleashedError error = parseError(e.getResponseBodyAsByteArray()).orElse(new UnleashedError(List.of()));
            LOGGER.debug("Parsed error: {}", error);

        }
        return Optional.empty();
    }

    private Optional<UnleashedError> parseError(byte[] responseBody) {

        try {
            return Optional.of(mapper.readValue(responseBody, UnleashedError.class));
        } catch (IOException e) {
            LOGGER.warn("Failed to parse error object: {}", new String(responseBody));
        }
        return Optional.empty();
    }

}
