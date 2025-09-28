package io.stock.sync.config;

import io.stock.sync.service.VendorClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "vendors.rest[0].name=VENDOR_A",
        "vendors.rest[0].url=http://example/a",
        "vendors.csv[0].name=VENDOR_B",
        "vendors.csv[0].path=/tmp/vendor-b/stock.csv"
})
class VendorClientsConfigTest {
    @Autowired
    List<VendorClient> clients;
    @Test
    void buildsClientsFromYaml() {
        assertThat(clients).extracting(VendorClient::vendorName)
                .containsExactlyInAnyOrder("VENDOR_A","VENDOR_B");
    }
}