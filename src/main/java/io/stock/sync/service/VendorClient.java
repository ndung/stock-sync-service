package io.stock.sync.service;

import io.stock.sync.service.dto.VendorProduct;

import java.util.List;

/** Strategy for pulling stock from a single vendor. */
public interface VendorClient {
    /**
     * A stable identifier used as the vendor field in DB (e.g., "VENDOR_A").
     */
    String vendorName();

    /**
     * Pull the full product list for this vendor. Must never throw; return empty on failure.
     */
    List<VendorProduct> fetch();
}