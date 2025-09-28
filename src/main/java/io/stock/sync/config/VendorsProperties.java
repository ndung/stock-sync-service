package io.stock.sync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "vendors")
public class VendorsProperties {
    private List<RestSpec> rest = new ArrayList<>();
    private List<CsvSpec> csv = new ArrayList<>();

    public List<RestSpec> getRest() { return rest; }
    public void setRest(List<RestSpec> rest) { this.rest = rest; }

    public List<CsvSpec> getCsv() { return csv; }
    public void setCsv(List<CsvSpec> csv) { this.csv = csv; }

    public static class RestSpec {
        private String name;   // e.g., VENDOR_A
        private String url;    // e.g., http://host/api/products
        private boolean enabled = true; // <--- NEW

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class CsvSpec {
        private String name;   // e.g., VENDOR_B
        private String path;   // e.g., /tmp/vendor-b/stock.csv
        private boolean enabled = true; // <--- NEW

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
