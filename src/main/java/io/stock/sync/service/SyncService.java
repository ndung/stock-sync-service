package io.stock.sync.service;

import io.stock.sync.model.Product;
import io.stock.sync.model.StockOutEvent;
import io.stock.sync.repository.ProductRepository;
import io.stock.sync.repository.StockOutEventRepository;
import io.stock.sync.service.dto.VendorProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    private final VendorAClient vendorAClient;
    private final VendorBReader vendorBReader;
    private final ProductRepository productRepository;
    private final StockOutEventRepository stockOutEventRepository;

    public SyncService(VendorAClient vendorAClient, VendorBReader vendorBReader,
                       ProductRepository productRepository, StockOutEventRepository stockOutEventRepository) {
        this.vendorAClient = vendorAClient;
        this.vendorBReader = vendorBReader;
        this.productRepository = productRepository;
        this.stockOutEventRepository = stockOutEventRepository;
    }

    @Transactional
    public void syncAll() {
        List<VendorProduct> combined = new ArrayList<>();
        combined.addAll(vendorAClient.fetch());
        combined.addAll(vendorBReader.fetch());
        processBatch(combined);
    }

    @Transactional
    public void processBatch(List<VendorProduct> vendorProducts) {
        for (VendorProduct vp : vendorProducts) {
            upsertAndDetect(vp);
        }
    }

    private void upsertAndDetect(VendorProduct vp) {
        Optional<Product> existingOpt = productRepository.findBySkuAndVendor(vp.sku(), vp.vendor());
        if (existingOpt.isEmpty()) {
            Product p = new Product(vp.sku(), vp.name(), vp.stockQuantity(), vp.vendor());
            productRepository.save(p);
            if (vp.stockQuantity() != null && vp.stockQuantity() == 0) {
                // initial snapshot zero does not count as a transition (spec ambiguous; we consider no event on first insert)
                log.info("Inserted product {}:{} with zero stock (no transition).", vp.vendor(), vp.sku());
            }
        } else {
            Product existing = existingOpt.get();
            Integer oldQty = existing.getStockQuantity();
            existing.setName(vp.name());
            existing.setStockQuantity(vp.stockQuantity());
            productRepository.save(existing);

            if (oldQty != null && oldQty > 0 && vp.stockQuantity() != null && vp.stockQuantity() == 0) {
                log.warn("STOCK-OUT detected for {}:{} ({} -> 0)", vp.vendor(), vp.sku(), oldQty);
                stockOutEventRepository.save(new StockOutEvent(existing.getSku(), existing.getVendor(), oldQty));
            }
        }
    }
}
