package net.jastrab.unleashedspringclient.client;

import net.jastrab.unleashed.api.GetProductRequest;
import net.jastrab.unleashed.api.models.*;
import net.jastrab.unleashedspringclient.UnleashedClientConfiguration;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@ActiveProfiles(profiles = {"integration"})
@ContextConfiguration
@SpringBootTest(classes = {UnleashedClientConfiguration.class})
class UnleashedClientIntegrationTest {

    @Autowired
    private UnleashedClient client;

    @Nested
    @DisplayName("Test Product resource creation, read, and update")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ProductIT {

        @Test
        @Order(1)
        @DisplayName("it successfully creates a new product")
        void createNewProduct() {
            final String productCode = UUID.randomUUID().toString();
            final String productDescription = "JUNIT GENERATED PRODUCT";
            final Product product = new Product(productCode);
            product.setProductDescription(productDescription);
            product.setBarcode(productCode);
            product.setWeight(10.5);
            product.setDepth(2.0);
            product.setHeight(3.1);
            product.setComponent(true);

            Product createdProduct = client.createItem(product, Product.class).orElseThrow();
            System.out.println(createdProduct);

            assertEquals(product.getGuid(), createdProduct.getGuid());
            assertEquals(productCode, createdProduct.getProductCode());
            assertEquals(productCode, createdProduct.getBarcode());
            assertEquals(10.5, createdProduct.getWeight());
        }

        @Test
        @Order(2)
        @DisplayName("it returns a specific test product with the correct values")
        void getListOfProducts() {
            final String productCode = "07-012";
            final String description = "Pump Air Shock";
            final String guid = "6c3a3990-5340-44f0-b67f-519c3b2bb335";


            GetProductRequest request = GetProductRequest.builder()
                    .productCode(productCode)
                    .build();

            List<Product> products = client.getItems(request);
            assertEquals(1, products.size());

            Product product = products.stream().findFirst().orElseThrow();

            assertEquals(guid, product.getGuid().toString());
            assertEquals(productCode, product.getProductCode());
            assertEquals(description, product.getProductDescription());
        }

        @Test
        @Disabled
        @DisplayName("it returns a complete set of products when pagination data is returned")
        void getCompleteProducts() {

            GetProductRequest request = GetProductRequest.builder()
                    .productGroup("AT1M")
                    .build();

            List<Product> products = client.getItems(request);

            assertEquals(274, products.size());
        }
    }

    @Test
    @DisplayName("it successfully returns a list of ProductGroups")
    void getProductGroups() {
        List<ProductGroup> groups = client.getProductGroups();

        assertTrue(groups.size() > 0);

        List<String> groupNames = groups.stream()
                .map(ProductGroup::getGroupName)
                .collect(Collectors.toList());
        assertTrue(groupNames.contains("Equipment"));
        assertTrue(groupNames.contains("Consumables"));
    }

    @Test
    @DisplayName("it successfully returns a list of AttributeSets")
    void getAttributeSets() {
        final List<AttributeSet> attributeSets = client.getAttributeSets();

        assertTrue(attributeSets.size() > 0);

        List<String> attributeSetNames = attributeSets.stream().map(AttributeSet::getName).collect(Collectors.toList());

        assertTrue(attributeSetNames.contains("Manufactured Component"));
    }

    @Test
    @DisplayName("it successfully returns a list of Payment Terms")
    void getPaymentTerms() {
        final List<String> paymentTermNames = client.getPaymentTerms().stream()
                .map(PaymentTerm::getName)
                .collect(Collectors.toList());

        assertTrue(paymentTermNames.size() > 0);

        final List<String> expectedSample = List.of("20th Month following", "7 days", "90 days", "On Delivery");

        expectedSample.forEach(value -> assertTrue(paymentTermNames.contains(value), "Missing value: " + value));
    }

    @Test
    @DisplayName("it successfully returns a list of Units of Measure")
    void getUnitsOfMeasure() {
        final List<String> uomNames = client.getUnitsOfMeasure().stream()
                .map(UnitOfMeasure::getName)
                .collect(Collectors.toList());

        assertTrue(uomNames.size() > 0);

        final List<String> expectedSample = List.of("EA", "Inch", "Meter");

        expectedSample.forEach(value -> assertTrue(uomNames.contains(value), "Missing value: " + value));
    }

    @Test
    @DisplayName("it successfully returns a list of Customer Types")
    void getCustomerTypes() {
        final List<String> custTypeNames = client.getCustomerTypes().stream()
                .map(CustomerType::getTypeName)
                .collect(Collectors.toList());

        assertEquals(5, custTypeNames.size());
    }

}
