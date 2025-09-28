package io.stock.sync.service;

import io.stock.sync.service.dto.VendorProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class RestVendorClient implements VendorClient {

    private static final Logger log = LoggerFactory.getLogger(RestVendorClient.class);

    private final String name;
    private final String url;
    private final RestTemplate restTemplate;
    private final RetryTemplate retry;

    public RestVendorClient(String name, String url, RestTemplate restTemplate) {
        this.name = name;
        this.url = url;
        this.restTemplate = restTemplate;
        this.retry = RetryTemplate.builder()
                .maxAttempts(3)
                .exponentialBackoff(1000, 2.0, 4000) // 1s -> 2s -> 4s
                .retryOn(RestClientException.class)
                .build();
    }

    @Override
    public String vendorName() { return name; }

    @Override
    public List<VendorProduct> fetch() {
        return retry.execute(ctx -> {
            log.info("Fetching {} from {}", name, url);
            ResponseEntity<Item[]> resp = restTemplate.getForEntity(url, Item[].class);
            Item[] body = resp.getBody();
            if (body == null) return List.of();
            return Arrays.stream(body)
                    .filter(Objects::nonNull)
                    .map(i -> new VendorProduct(i.sku, i.name, i.stockQuantity, name))
                    .toList();
        }, ctx -> {
            log.error("Fetch failed for {} after {} attempts: {}",
                    name, ctx.getRetryCount(), ctx.getLastThrowable() != null ? ctx.getLastThrowable().getMessage() : "unknown");
            return List.of();
        });
    }

    // matches expected vendor JSON fields
    static class Item { public String sku; public String name; public Integer stockQuantity; }
}
