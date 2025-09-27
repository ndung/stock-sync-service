package io.stock.sync.service;

import io.stock.sync.service.dto.VendorProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class VendorAClient implements VendorClient {

    private static final Logger log = LoggerFactory.getLogger(VendorAClient.class);

    private final RestTemplate restTemplate;
    private final String vendorAUrl;

    public VendorAClient(RestTemplate restTemplate,
                         @Value("${vendorA.url:http://localhost:8080/mock/vendor-a/products}") String vendorAUrl) {
        this.restTemplate = restTemplate;
        this.vendorAUrl = vendorAUrl;
    }

    @Override
    public String vendorName() {
        return "A";
    }

    @Retryable(
            retryFor = { RestClientException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public List<VendorProduct> fetch() {
        log.info("Fetching Vendor A products from {}", vendorAUrl);
        ResponseEntity<VendorAItem[]> resp = restTemplate.getForEntity(vendorAUrl, VendorAItem[].class);
        VendorAItem[] body = resp.getBody();
        if (body == null) return List.of();
        return Arrays.stream(body)
                .filter(Objects::nonNull)
                .map(i -> new VendorProduct(i.sku, i.name, i.stockQuantity, "VENDOR_A"))
                .collect(Collectors.toList());
    }

    @Recover
    public List<VendorProduct> recover(RestClientException ex) {
        log.error("Vendor A fetch failed after retries: {}", ex.getMessage());
        return List.of(); // degrade gracefully
    }

    static class VendorAItem {
        public String sku;
        public String name;
        public Integer stockQuantity;
    }
}
