package io.stock.sync.service;

import io.stock.sync.service.dto.VendorProduct;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class VendorBReader {

    private static final Logger log = LoggerFactory.getLogger(VendorBReader.class);

    private final String csvPath;

    public VendorBReader(@Value("${vendorB.csvPath:/tmp/vendor-b/stock.csv}") String csvPath) {
        this.csvPath = csvPath;
    }

    public List<VendorProduct> fetch() {
        File f = new File(csvPath);
        if (!f.exists()) {
            log.warn("Vendor B CSV not found at {}, returning empty list", csvPath);
            return List.of();
        }
        List<VendorProduct> list = new ArrayList<>();
        try (Reader reader = new FileReader(f, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader("sku","name","stockQuantity")
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {
            for (CSVRecord rec : parser) {
                String sku = rec.get("sku").trim();
                String name = rec.get("name").trim();
                Integer qty = Integer.valueOf(rec.get("stockQuantity").trim());
                list.add(new VendorProduct(sku, name, qty, "VENDOR_B"));
            }
        } catch (Exception e) {
            log.error("Failed reading Vendor B CSV at {}: {}", csvPath, e.getMessage(), e);
        }
        return list;
    }
}
