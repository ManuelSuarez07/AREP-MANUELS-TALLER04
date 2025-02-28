package com.eci.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController 
public class GreetingController {

    @GetMapping("/greeting")
    public String getGreeting(@RequestParam(value = "name", defaultValue = "World") String name) {
        return String.format("{\"message\": \"Hello, %s! Welcome to CurrencyConverter!\"}", name);
    }
}