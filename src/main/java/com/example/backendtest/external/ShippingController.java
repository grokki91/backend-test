package com.example.backendtest.external;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shipping")
public class ShippingController {

    private final ShippingClient shippingClient;

    public ShippingController(ShippingClient shippingClient) {
        this.shippingClient = shippingClient;
    }

    @GetMapping("/quote")
    public Map<String, Object> quote(@RequestParam(defaultValue = "berlin") String destination) {
        return shippingClient.quote(destination);
    }
}
