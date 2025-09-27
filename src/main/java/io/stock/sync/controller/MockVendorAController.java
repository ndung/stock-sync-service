package io.stock.sync.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/mock/vendor-a")
public class MockVendorAController {

    @GetMapping(value = "/products", produces = MediaType.APPLICATION_JSON_VALUE)
    public String products() throws IOException {
        var res = new ClassPathResource("vendor-a-sample.json");
        byte[] bytes = FileCopyUtils.copyToByteArray(res.getInputStream());
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
