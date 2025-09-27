package io.stock.sync.service.dto;

public record VendorProduct(String sku, String name, Integer stockQuantity, String vendor) {}
