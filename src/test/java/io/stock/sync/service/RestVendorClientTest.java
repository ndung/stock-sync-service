package io.stock.sync.service;

import io.stock.sync.service.dto.VendorProduct;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RestVendorClientTest {

    private MockWebServer server;
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        restTemplate = new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(2))
                .build();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

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

        String url = server.url("/api/products").toString();
        RestVendorClient client = new RestVendorClient("VENDOR_A", url, restTemplate);

        List<VendorProduct> result = client.fetch();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).sku()).isEqualTo("ABC123");
        assertThat(result.get(0).name()).isEqualTo("Product A");
        assertThat(result.get(0).stockQuantity()).isEqualTo(8);
        assertThat(result.get(0).vendor()).isEqualTo("VENDOR_A");
        assertThat(result.get(1).sku()).isEqualTo("LMN789");
        assertThat(result.get(1).stockQuantity()).isZero();
    }

    @Test
    void fetch_retriesOnFailure_thenSucceeds() {
        // First attempt: 500 -> triggers retry inside RestVendorClient (RetryTemplate)
        server.enqueue(new MockResponse().setResponseCode(500));
        // Second attempt: success
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    [
                      { "sku": "ABC123", "name": "Product A", "stockQuantity": 8 }
                    ]
                """));

        String url = server.url("/api/products").toString();
        RestVendorClient client = new RestVendorClient("VENDOR_C", url, restTemplate);

        List<VendorProduct> result = client.fetch();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).sku()).isEqualTo("ABC123");
        assertThat(result.get(0).vendor()).isEqualTo("VENDOR_C");
    }

    @Test
    void fetch_retriesExhausted_returnsEmpty() {
        // Enqueue three failures to exceed maxAttempts=3 in RestVendorClient
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse().setResponseCode(502));
        server.enqueue(new MockResponse().setResponseCode(503));

        String url = server.url("/api/products").toString();
        RestVendorClient client = new RestVendorClient("VENDOR_X", url, restTemplate);

        List<VendorProduct> result = client.fetch();

        assertThat(result).isEmpty();
    }
}
