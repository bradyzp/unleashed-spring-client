package net.jastrab.unleashedspringclient.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import net.jastrab.unleashed.api.GetProductRequest;
import net.jastrab.unleashed.api.http.PaginatedUnleashedRequest;
import net.jastrab.unleashed.api.models.Product;
import net.jastrab.unleashedspringclient.UnleashedClientConfiguration;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("integration")
@ActiveProfiles(profiles = {"integration"})
@ContextConfiguration(initializers = WireMockProductsIntegrationTest.BaseUriInitializer.class)
@SpringBootTest(classes = {UnleashedClientConfiguration.class})
public class WireMockProductsIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(WireMockProductsIntegrationTest.class);
    private static WireMockServer server;

    @Autowired
    private UnleashedClient client;

    @BeforeAll
    static void beforeAll() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        LOGGER.info("WireMock Server started on port {}", server.port());
    }

    static class BaseUriInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues.of("unleashed.client.base-uri=http://localhost:" + server.port())
                    .applyTo(applicationContext.getEnvironment());
        }
    }

    @BeforeEach
    void beforeEach() {
        // Return 400 for any un-stubbed/un-matched requests
        server.stubFor(any(anyUrl())
                .atPriority(10)
                .willReturn(aResponse().withStatus(400).withBody("Invalid (unmatched) Request")));
    }

    @AfterAll
    static void afterAll() {
        server.stop();
        LOGGER.info("WireMock Server shutdown");
    }

    @Test
    @DisplayName("Test get product request for single product with WireMock")
    void testGetSingleProductRequest() {
        server.stubFor(get(urlPathEqualTo("/Products/"))
                .withQueryParam("productCode", equalTo("07-012"))
                .withHeader("Accept", equalTo("application/json"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBodyFile("product_07-012.json")
                ));

        PaginatedUnleashedRequest<Product> productRequest = GetProductRequest.builder().productCode("07-012").build();

        final Product product = client.getItem(productRequest).orElseThrow();

        assertEquals("Pump Air Shock", product.getProductDescription());
        assertEquals(BigDecimal.valueOf(33.95), product.getAverageLandPrice());
    }

    @Disabled("Auto-pagination disabled")
    @Test
    @DisplayName("Test get product request for a paginated response")
    void testGetProductRequestPaging() {
        // Stubbed by MultiPageProduct.json

        final GetProductRequest request = GetProductRequest.builder()
                .productGroup("Tools")
                .build();

        final List<Product> products = client.getItems(request);

        assertEquals(3, products.size());
        assertEquals("Third Tool", products.get(2).getProductDescription());

        server.verify(2, getRequestedFor(urlPathMatching("/Products/Page/[2-3]")));
    }

}
