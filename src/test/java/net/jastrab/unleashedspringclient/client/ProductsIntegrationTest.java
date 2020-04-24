package net.jastrab.unleashedspringclient.client;

import net.jastrab.unleashed.api.GetProductRequest;
import net.jastrab.unleashed.api.models.Product;
import net.jastrab.unleashedspringclient.UnleashedClientConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.Optional;

@Tag("integration")
@ActiveProfiles(profiles = {"integration"})
@ContextConfiguration
@SpringBootTest(classes = {UnleashedClientConfiguration.class})
public class ProductsIntegrationTest {

    @Autowired
    private UnleashedClient client;

    @Test
    @DisplayName("Test retrieval of product with illegal url character in productName")
    void testGetProductUrlEscaping() {

        GetProductRequest request = GetProductRequest.builder().productCode("LT6657AHMS8-1.25#PBF").build();

        Optional<Product> productOptional = client.getItem(request);
        productOptional.ifPresent(product -> System.out.println("Got product: " + product));


    }

}
