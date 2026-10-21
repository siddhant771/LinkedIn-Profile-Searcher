package com.example.demo;



import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class PhantomClient {

    private final ObjectMapper mapper;
    private final String key;
    private final String agent;
    private final String argument;
    private final String resultPointer;
    private final String resultUrlPointer;
    private final String resultHost;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public PhantomClient(
            ObjectMapper mapper,
            @Value("${phantom.key}") String key,
            @Value("${phantom.agent}") String agent,
            @Value("${phantom.argument}") String argument,
            @Value("${phantom.result-pointer}") String resultPointer,
            @Value("${phantom.result-url-pointer}") String resultUrlPointer,
            @Value("${phantom.result-host}") String resultHost
    ) {
        this.mapper = mapper;
        this.key = key;
        this.agent = agent;
        this.argument = argument;
        this.resultPointer = resultPointer;
        this.resultUrlPointer = resultUrlPointer;
        this.resultHost = resultHost;
    }

    public List<AlumniProfile> search(SearchRequest request) {
        if (key.isBlank() || agent.isBlank()) {
            throw error(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Configure PhantomBuster key and agent ID"
            );
        }

        long deadline = System.nanoTime()
                + Duration.ofSeconds(45).toNanos();

        try {
            String keywords = "\"" + request.university() + "\" "
                    + request.designation();

            String searchUrl =
                    "https://www.linkedin.com/search/results/people/?keywords="
                            + URLEncoder.encode(keywords, StandardCharsets.UTF_8);

            JsonNode launch = call(
                    "/agents/launch",
                    Map.of(
                            "id", agent,
                            "saveArgument", false,
                            "maxInstanceCount", 1,
                            "bonusArgument", Map.of(argument, searchUrl)
                    ),
                    deadline
            );

            String id = launch.path("containerId").asText("");

            if (!id.matches("[A-Za-z0-9_-]+")) {
                throw error(
                        HttpStatus.BAD_GATEWAY,
                        "Launch returned no valid container ID"
                );
            }

            while (true) {
                checkDeadline(deadline);

                JsonNode state = call(
                        "/containers/fetch?id=" + id,
                        null,
                        deadline
                );

                JsonNode exit = state.get("exitCode");

                if (exit != null && !exit.isNull()) {
                    if (!exit.isIntegralNumber() || exit.asInt() != 0) {
                        throw error(
                                HttpStatus.BAD_GATEWAY,
                                "Phantom failed; inspect its dashboard"
                        );
                    }

                    break;
                }

                Thread.sleep(1000);
            }

            JsonNode result = call(
                    "/containers/fetch-result-object?id=" + id,
                    null,
                    deadline
            );

            if (!resultUrlPointer.isBlank()) {
                URI url = URI.create(
                        result.at(resultUrlPointer).asText("")
                );

                if (!"https".equalsIgnoreCase(url.getScheme())
                        || resultHost.isBlank()
                        || !resultHost.equalsIgnoreCase(url.getHost())
                        || url.getUserInfo() != null
                        || url.getPort() != -1) {
                    throw error(
                            HttpStatus.BAD_GATEWAY,
                            "Result URL host is not allowed"
                    );
                }

                result = send(
                        HttpRequest.newBuilder(url)
                                .timeout(timeout(deadline))
                                .GET()
                                .build()
                );
            }

            if (result.isTextual()) {
                result = mapper.readTree(result.asText());
            }

            JsonNode rows = result.at(resultPointer);

            if (!rows.isArray() || rows.size() > 100) {
                throw error(
                        HttpStatus.BAD_GATEWAY,
                        "Expected an array of at most 100 profiles"
                );
            }

            List<AlumniProfile> profiles = new ArrayList<>();

            for (JsonNode row : rows) {
                AlumniProfile p = mapper.treeToValue(
                        row,
                        AlumniProfile.class
                );

                if (blank(p.name())
                        || blank(p.currentRole())
                        || blank(p.university())
                        || blank(p.linkedinUrl())) {
                    throw error(
                            HttpStatus.BAD_GATEWAY,
                            "Missing profile fields; verify mapping/enrichment"
                    );
                }

                if (p.passoutYear() != null
                        && (p.passoutYear() < 1900
                        || p.passoutYear() > 2100)) {
                    throw error(
                            HttpStatus.BAD_GATEWAY,
                            "Invalid graduation year"
                    );
                }

                profiles.add(
                        new AlumniProfile(
                                p.name(),
                                p.currentRole(),
                                p.university(),
                                p.location(),
                                p.linkedinHeadline(),
                                p.passoutYear(),
                                canonicalUrl(p.linkedinUrl())
                        )
                );
            }

            return profiles;

        } catch (ResponseStatusException e) {
            throw e;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw error(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Search interrupted"
            );

        } catch (HttpTimeoutException e) {
            throw error(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "Provider timed out; external work may continue"
            );

        } catch (Exception e) {
            throw error(
                    HttpStatus.BAD_GATEWAY,
                    "Provider request or JSON mapping failed"
            );
        }
    }

    private JsonNode call(
            String path,
            Object body,
            long deadline
    ) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(
                        URI.create(
                                "https://api.phantombuster.com/api/v2" + path
                        )
                )
                .timeout(timeout(deadline))
                .header("X-Phantombuster-Key", key)
                .header("Accept", "application/json");

        if (body == null) {
            request.GET();
        } else {
            request.header("Content-Type", "application/json")
                    .POST(
                            HttpRequest.BodyPublishers.ofString(
                                    mapper.writeValueAsString(body)
                            )
                    );
        }

        return send(request.build());
    }

    private JsonNode send(HttpRequest request) throws Exception {
        HttpResponse<String> response = http.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() == 429) {
            throw error(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Provider rate limit reached"
            );
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw error(
                    HttpStatus.BAD_GATEWAY,
                    "Provider returned HTTP " + response.statusCode()
            );
        }

        JsonNode result = mapper.readTree(response.body());

        if (result == null) {
            throw error(
                    HttpStatus.BAD_GATEWAY,
                    "Empty provider response"
            );
        }

        return result;
    }

    private String canonicalUrl(String value) {
        URI uri = URI.create(value.strip());
        String host = uri.getHost();
        String path = uri.getPath();

        if (!"https".equalsIgnoreCase(uri.getScheme())
                || host == null
                || !(host.equalsIgnoreCase("linkedin.com")
                || host.toLowerCase(Locale.ROOT).endsWith(".linkedin.com"))
                || path == null
                || !path.matches("/in/[^/]+/?")
                || uri.getUserInfo() != null
                || uri.getPort() != -1) {
            throw error(
                    HttpStatus.BAD_GATEWAY,
                    "Invalid LinkedIn URL"
            );
        }

        return "https://www.linkedin.com" + path.replaceAll("/$", "");
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private void checkDeadline(long deadline) {
        if (System.nanoTime() >= deadline) {
            throw error(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "Search timed out; external work may continue"
            );
        }
    }

    private Duration timeout(long deadline) {
        long remaining = deadline - System.nanoTime();

        if (remaining <= 0) {
            throw error(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "Search timed out; external work may continue"
            );
        }

        return Duration.ofNanos(
                Math.min(remaining, Duration.ofSeconds(10).toNanos())
        );
    }

    private ResponseStatusException error(
            HttpStatus status,
            String message
    ) {
        return new ResponseStatusException(status, message);
    }
}
