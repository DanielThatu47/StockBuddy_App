// src/main/java/com/stockbuddy/controller/RootController.java
package com.stockbuddy.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    @GetMapping("/")
    public String root() {
        return "API is running";
    }
}
