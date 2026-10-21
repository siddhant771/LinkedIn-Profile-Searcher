package com.example.demo;


import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alumni")
public class AlumniController {

    private final AlumniService service;

    public AlumniController(AlumniService service) {
        this.service = service;
    }

    @PostMapping("/search")
    public Map<String, Object> search(
            @Valid @RequestBody SearchRequest request
    ) {
        return Map.of(
                "status", "success",
                "data", service.search(request)
        );
    }

    @GetMapping("/all")
    public Map<String, Object> findAll(
            @RequestParam(defaultValue = "0")
            @Min(0) @Max(100000) int page,

            @RequestParam(defaultValue = "20")
            @Min(1) @Max(100) int size
    ) {
        return Map.of(
                "status", "success",
                "data", service.findAll(page, size)
        );
    }
}