package io.stock.sync.service;

import io.stock.sync.service.dto.VendorProduct;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvVendorClientTest {

    @TempDir
    Path tmp;

    @Test
    void fetch_readsValidCsv() throws IOException {
        Path csv = tmp.resolve("stock.csv");
        Files.writeString(csv, """
            sku,name,stockQuantity
            ABC123,Product A,10
            XYZ456,Product B,0
            """);

        CsvVendorClient client = new CsvVendorClient("VENDOR_B", csv.toString());

        List<VendorProduct> products = client.fetch();

        assertThat(products).hasSize(2);

        VendorProduct p1 = products.get(0);
        assertThat(p1.sku()).isEqualTo("ABC123");
        assertThat(p1.name()).isEqualTo("Product A");
        assertThat(p1.stockQuantity()).isEqualTo(10);
        assertThat(p1.vendor()).isEqualTo("VENDOR_B");

        VendorProduct p2 = products.get(1);
        assertThat(p2.sku()).isEqualTo("XYZ456");
        assertThat(p2.stockQuantity()).isEqualTo(0);
    }

    @Test
    void fetch_missingFile_returnsEmptyList() {
        Path csv = tmp.resolve("does-not-exist.csv");

        CsvVendorClient client = new CsvVendorClient("VENDOR_B", csv.toString());

        List<VendorProduct> products = client.fetch();

        assertThat(products).isEmpty();
    }

    @Test
    void fetch_malformedRow_returnsParsedSoFar() throws IOException {
        Path csv = tmp.resolve("bad.csv");
        // Second row has non-integer stockQuantity → NumberFormatException during iteration
        Files.writeString(csv, """
            sku,name,stockQuantity
            GOOD1,Good Product,5
            BAD99,Bad Product,notanint
            """);

        CsvVendorClient client = new CsvVendorClient("VENDOR_B", csv.toString());

        List<VendorProduct> products = client.fetch();

        // Implementation logs the error and returns only successfully parsed records
        assertThat(products).hasSize(1);
        assertThat(products.get(0).sku()).isEqualTo("GOOD1");
        assertThat(products.get(0).stockQuantity()).isEqualTo(5);
    }
}
