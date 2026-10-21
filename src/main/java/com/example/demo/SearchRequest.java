package com.example.demo;
import jakarta.validation.constraints.*;
public record SearchRequest(
        @NotBlank @Size(max = 200) String university,
        @NotBlank @Size(max = 120) String designation,
        @Min(1900) @Max(2100) Integer passoutYear
) {
}