package io.stock.sync;

import io.stock.sync.service.VendorBReader;
import io.stock.sync.service.dto.VendorProduct;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VendorBReaderTest {

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

        VendorBReader reader = new VendorBReader(csv.toString());

        List<VendorProduct> products = reader.fetch();

        assertThat(products).hasSize(2);
        assertThat(products.get(0).sku()).isEqualTo("ABC123");
        assertThat(products.get(0).name()).isEqualTo("Product A");
        assertThat(products.get(0).stockQuantity()).isEqualTo(10);
        assertThat(products.get(0).vendor()).isEqualTo("VENDOR_B");
        assertThat(products.get(1).sku()).isEqualTo("XYZ456");
        assertThat(products.get(1).stockQuantity()).isZero();
    }

    @Test
    void fetch_missingFile_returnsEmptyList() {
        // point to a non-existent file
        Path csv = tmp.resolve("does-not-exist.csv");

        VendorBReader reader = new VendorBReader(csv.toString());

        List<VendorProduct> products = reader.fetch();

        assertThat(products).isEmpty();
    }

    @Test
    void fetch_malformedRow_logsAndReturnsPartial() throws IOException {
        Path csv = tmp.resolve("bad.csv");
        // second row has non-integer stockQuantity → NumberFormatException
        Files.writeString(csv, """
            sku,name,stockQuantity
            GOOD1,Good Product,5
            BAD99,Bad Product,notanint
            """);

        VendorBReader reader = new VendorBReader(csv.toString());

        List<VendorProduct> products = reader.fetch();

        // Implementation logs an error and returns whatever was parsed before the failure
        assertThat(products).hasSize(1);
        assertThat(products.get(0).sku()).isEqualTo("GOOD1");
        assertThat(products.get(0).stockQuantity()).isEqualTo(5);
    }
}
