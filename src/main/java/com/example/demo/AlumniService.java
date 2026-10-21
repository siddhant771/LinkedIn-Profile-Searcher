package com.example.demo;



import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class AlumniService {

    private final PhantomClient client;
    private final AlumniRepository repository;

    public AlumniService(
            PhantomClient client,
            AlumniRepository repository
    ) {
        this.client = client;
        this.repository = repository;
    }

    public List<AlumniProfile> search(SearchRequest request) {
        Map<String, AlumniProfile> unique = new LinkedHashMap<>();

        for (AlumniProfile p : client.search(request)) {
            boolean universityMatches =
                    clean(p.university()).equals(clean(request.university()));

            boolean roleMatches =
                    clean(p.currentRole()).contains(clean(request.designation()));

            boolean yearMatches =
                    request.passoutYear() == null
                            || request.passoutYear().equals(p.passoutYear());

            if (universityMatches && roleMatches && yearMatches) {
                unique.put(p.linkedinUrl(), p);
            }
        }

        List<AlumniProfile> results = List.copyOf(unique.values());
        repository.saveAll(results);
        return results;
    }

    public List<AlumniProfile> findAll(int page, int size) {
        return repository.findAll(page, size);
    }

    private String clean(String value) {
        return value == null ? "" :
                value.strip()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }
}
