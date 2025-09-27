package io.stock.sync;

import io.stock.sync.service.VendorAClient;
import io.stock.sync.service.dto.VendorProduct;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
@SpringBootTest(classes = Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
class VendorAClientTest {

    static final MockWebServer server = new MockWebServer();
    static {
        try { server.start(); } catch (IOException e) { throw new RuntimeException(e); }
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry reg) {
        reg.add("vendorA.url", () -> server.url("/vendor-a/products").toString());
    }

    @AfterAll
    static void shutdown() throws IOException {
        server.shutdown();
    }

    @Autowired
    VendorAClient client;

    @Test
    void fetch_success_parsesProducts() {
        String body = """
            [
              { "sku": "ABC123", "name": "Product A", "stockQuantity": 8 },
              { "sku": "LMN789", "name": "Product C", "stockQuantity": 0 }
            ]
            """;
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body));

        List<VendorProduct> products = client.fetch();

        assertThat(products).hasSize(2);
        assertThat(products.get(0).sku()).isEqualTo("ABC123");
        assertThat(products.get(0).vendor()).isEqualTo("VENDOR_A");
        assertThat(products.get(1).stockQuantity()).isZero();
    }

    @Test
    void fetch_retriesOnFailure_thenSucceeds() {
        // First attempt fails (500), second succeeds (200)
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    [
                      { "sku": "ABC123", "name": "Product A", "stockQuantity": 8 }
                    ]
                """));

        List<VendorProduct> products = client.fetch();
        assertThat(products).hasSize(1);
        assertThat(products.get(0).sku()).isEqualTo("ABC123");
    }

}

