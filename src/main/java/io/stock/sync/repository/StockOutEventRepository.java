package io.stock.sync.repository;

import io.stock.sync.model.StockOutEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockOutEventRepository extends JpaRepository<StockOutEvent, Long> {
}
