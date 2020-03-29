package net.jastrab.unleashedspringclient.client;

import net.jastrab.unleashed.api.GetSupplierRequest;
import net.jastrab.unleashed.api.models.Supplier;
import net.jastrab.unleashedspringclient.UnleashedClientConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
@ActiveProfiles(profiles = {"integration"})
@ContextConfiguration
@SpringBootTest(classes = {UnleashedClientConfiguration.class})
public class SuppliersIntegrationTest {

    @Autowired
    private UnleashedClient client;

    @Test
    @DisplayName("Test retrieval of all suppliers")
    void testGetAllSuppliers() {
        GetSupplierRequest request = GetSupplierRequest.builder().build();

        List<Supplier> suppliers = client.getItems(request);

        assertTrue(suppliers.size() > 100);
    }

    @Test
    @DisplayName("Test retrieval of specific supplier by supplier code")
    void testGetSingleSupplier() {
        GetSupplierRequest request = GetSupplierRequest.builder()
                .supplierCode("DIGI")
                .build();

        Optional<Supplier> digikeySupplier = client.getItem(request);
        assertTrue(digikeySupplier.isPresent());

        digikeySupplier.ifPresent(supplier -> {
            assertEquals("Digikey", supplier.getSupplierName());
            assertEquals("sales@digikey.com", supplier.getEmail());
            assertEquals("https://www.digikey.com", supplier.getWebsite());
        });

    }

    @Test
    @DisplayName("Test search of suppliers by start of contact email address")
    void testGetSupplierByEmail() {
        GetSupplierRequest request = GetSupplierRequest.builder()
                .contactEmail("sales")
                .build();

        List<Supplier> suppliers = client.getItems(request);

        assertTrue(suppliers.size() >= 1);
    }


}
