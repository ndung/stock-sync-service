package io.stock.sync.repository;

import io.stock.sync.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySkuAndVendor(String sku, String vendor);
}
