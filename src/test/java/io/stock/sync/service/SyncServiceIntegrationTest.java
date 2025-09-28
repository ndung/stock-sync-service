package io.stock.sync.service;

import io.stock.sync.model.Product;
import io.stock.sync.model.StockOutEvent;
import io.stock.sync.repository.ProductRepository;
import io.stock.sync.repository.StockOutEventRepository;
import io.stock.sync.service.SyncService;
import io.stock.sync.service.dto.VendorProduct;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SyncServiceIntegrationTest {

    @Autowired
    SyncService syncService;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    StockOutEventRepository eventRepository;

    @Test
    void createsStockOutEventOnTransitionToZero() {
        // seed with positive stock
        Product p = new Product("ZERO123", "Will drop to zero", 5, "VENDOR_A");
        productRepository.save(p);

        // process batch with zero
        List<VendorProduct> batch = List.of(
                new VendorProduct("ZERO123", "Will drop to zero", 0, "VENDOR_A")
        );
        syncService.processBatch(batch);

        List<StockOutEvent> events = eventRepository.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getSku()).isEqualTo("ZERO123");
        assertThat(events.get(0).getPreviousQuantity()).isEqualTo(5);

        // product updated
        Product updated = productRepository.findBySkuAndVendor("ZERO123", "VENDOR_A").orElseThrow();
        assertThat(updated.getStockQuantity()).isEqualTo(0);
    }
}
