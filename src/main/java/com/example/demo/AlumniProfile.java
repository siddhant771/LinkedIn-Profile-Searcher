package com.example.demo;


import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AlumniProfile(
        @JsonAlias("fullName") String name,
        String currentRole,
        String university,
        String location,
        @JsonAlias("headline") String linkedinHeadline,
        Integer passoutYear,
        @JsonAlias("profileUrl") String linkedinUrl
) {
}