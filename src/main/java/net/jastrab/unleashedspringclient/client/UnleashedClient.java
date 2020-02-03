package net.jastrab.unleashedspringclient.client;

import net.jastrab.unleashed.api.CreateItemRequest;
import net.jastrab.unleashed.api.SimpleGetRequest;
import net.jastrab.unleashed.api.http.CreatableResource;
import net.jastrab.unleashed.api.http.PaginatedUnleashedRequest;
import net.jastrab.unleashed.api.http.UnleashedRequest;
import net.jastrab.unleashed.api.models.*;
import net.jastrab.unleashed.api.security.ApiCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class UnleashedClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnleashedClient.class);
    private final RestTemplate restTemplate;
    private final ApiCredential credential;
    private String apiAuthority = "api.unleashedsoftware.com";
    private String apiScheme = "https";

    public UnleashedClient(final ApiCredential credential,
                           final RestTemplateBuilder builder,
                           final MappingJackson2HttpMessageConverter converter) {
        this.restTemplate = builder
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .messageConverters(converter)
                .build();

        this.credential = credential;
        LOGGER.debug("UnleashedClient successfully initialized with API credentials");
    }

    public <T> List<T> getItems(PaginatedUnleashedRequest<T> request) {
        return getItems(request, false);
    }

    /**
     * Request a list of items from Unleashed API. The response may be paginated.
     *
     * @param request The PaginatedUnleashedRequest for the item
     * @param <T> type of the item that will be returned from the request
     * @return List<T> containing the items retrieved from the API, potentially empty
     */
    public <T> List<T> getItems(PaginatedUnleashedRequest<T> request, boolean fetchAll) {
        Objects.requireNonNull(request, "Request cannot be null");
        LOGGER.debug("Performing getItems request URI: {} type <{}>", request.getUri(), request.getResponseType());

        final Optional<UnleashedResponse<T>> response = this.exchange(request);
        final List<T> items = response.map(UnleashedResponse::getItems).orElse(new ArrayList<>());

        if(fetchAll) {
            // Retrieve further pages
            response.flatMap(UnleashedResponse::getPagination).ifPresent(pagination -> {
                LOGGER.debug("Response pagination: {}", pagination);
                final int pageNumber = pagination.getPageNumber();
                if (pageNumber != pagination.getNumberOfPages()) {
                    PaginatedUnleashedRequest<T> pageRequest = request.forPage(pageNumber + 1);
                    List<T> nextPage = getItems(pageRequest, true);
                    items.addAll(nextPage);
                }
            });
        }
//        response.flatMap(UnleashedResponse::getPagination)
//                .filter(p -> p.getNumberOfPages() > 1)
//                .map(Pagination::getNumberOfPages)
//                .map(pages -> IntStream.range(2, pages).mapToObj(request::forPage))
//                .map(this::getItems).

        return items;
    }

    /**
     * Get a single item from a request. Useful utility for requests which are only expected to return a single result.
     *
     * If more than one result is returned by the request, this method will return only the first
     *
     * @param request The PaginatedUnleashedRequest for the item
     * @param <T> type of the item that will be returned by the request
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

    public void setApiAuthority(String apiAuthority) {
        this.apiAuthority = apiAuthority;
    }

    public void setApiScheme(String apiScheme) {
        this.apiScheme = apiScheme;
    }

    private <T, R> Optional<R> exchange(UnleashedRequest<T> request) {
        if (!request.isSigned()) {
            LOGGER.debug("Signing request with API Credentials");
            request.sign(this.credential);
        }
        request.setAuthority(apiAuthority);
        request.setScheme(apiScheme);

        LOGGER.debug("Request URI: {}", request.getUri());
        final HttpMethod method = HttpMethod.valueOf(request.getHttpMethod().name());
        LOGGER.debug("Request method: {}", method);
        final HttpHeaders headers = new HttpHeaders(new LinkedMultiValueMap<>(request.getHeaders()));
        LOGGER.debug("Request headers: {}", headers);


        final ResponseEntity<R> response = this.restTemplate.exchange(
                request.getUri(),
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
    }

}
