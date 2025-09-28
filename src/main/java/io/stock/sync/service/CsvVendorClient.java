package io.stock.sync.service;

import io.stock.sync.service.dto.VendorProduct;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CsvVendorClient implements VendorClient {

    private static final Logger log = LoggerFactory.getLogger(CsvVendorClient.class);

    private final String name;
    private final String path;

    public CsvVendorClient(String name, String path) {
        this.name = name;
        this.path = path;
    }

    @Override
    public String vendorName() { return name; }

    @Override
    public List<VendorProduct> fetch() {
        File f = new File(path);
        if (!f.exists()) {
            log.warn("CSV for {} not found at {}, returning empty", name, path);
            return List.of();
        }
        List<VendorProduct> list = new ArrayList<>();
        try (Reader reader = new FileReader(f, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader("sku","name","stockQuantity")
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {
            for (CSVRecord rec : parser) {
                String sku = rec.get("sku").trim();
                String pname = rec.get("name").trim();
                Integer qty = Integer.valueOf(rec.get("stockQuantity").trim());
                list.add(new VendorProduct(sku, pname, qty, name));
            }
        } catch (Exception e) {
            log.error("CSV read error for {} at {}: {}", name, path, e.getMessage(), e);
        }
        return list;
    }
}
