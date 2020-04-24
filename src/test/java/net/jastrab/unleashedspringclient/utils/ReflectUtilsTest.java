package net.jastrab.unleashedspringclient.utils;

import net.jastrab.unleashed.api.models.Product;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReflectUtilsTest {

    @Test
    void testUpdateBeanUtil() {
        Product product1 = new Product("ABC-123");
        product1.setBarcode("123456");
        product1.setProductDescription("Product 1 Original Description");

        Product product2 = new Product("ABC-123");
        product2.setBarcode("78901");

        ReflectUtils.updateEntity(product1, product2);

        assertEquals("Product 1 Original Description", product1.getProductDescription());
        assertEquals("78901", product1.getBarcode());
    }

}
