package io.stock.sync.config;


import io.stock.sync.service.CsvVendorClient;
import io.stock.sync.service.RestVendorClient;
import io.stock.sync.service.VendorClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableConfigurationProperties(VendorsProperties.class)
public class VendorClientsConfig {

    @Bean
    public List<VendorClient> vendorClients(RestTemplate restTemplate, VendorsProperties props) {
        List<VendorClient> all = new ArrayList<>();

        // REST vendors
        for (var r : props.getRest()) {
            if (r.isEnabled()) {
                all.add(new RestVendorClient(r.getName(), r.getUrl(), restTemplate));
            }
        }
        // CSV vendors
        for (var c : props.getCsv()) {
            if (c.isEnabled()) {
                all.add(new CsvVendorClient(c.getName(), c.getPath()));
            }
        }
        return all;
    }
}
