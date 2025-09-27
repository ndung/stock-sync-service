package io.stock.sync.model;


import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "stock_out_events", indexes = {
        @Index(name = "idx_sku_vendor_time", columnList = "sku,vendor,occurredAt")
})
public class StockOutEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private String vendor;

    @Column(nullable = false)
    private Integer previousQuantity;

    @Column(nullable = false)
    private OffsetDateTime occurredAt;

    public StockOutEvent() {}

    public StockOutEvent(String sku, String vendor, Integer previousQuantity) {
        this.sku = sku;
        this.vendor = vendor;
        this.previousQuantity = previousQuantity;
        this.occurredAt = OffsetDateTime.now();
    }

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }

    public Integer getPreviousQuantity() { return previousQuantity; }
    public void setPreviousQuantity(Integer previousQuantity) { this.previousQuantity = previousQuantity; }

    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(OffsetDateTime occurredAt) { this.occurredAt = occurredAt; }
}
