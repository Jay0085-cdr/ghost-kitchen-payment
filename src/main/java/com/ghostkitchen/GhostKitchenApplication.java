package com.ghostkitchen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GhostKitchenApplication {

    public static void main(String[] args) {
        SpringApplication.run(GhostKitchenApplication.class, args);
    }
}
