package com.example.demo;



import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AlumniServiceTest {

    @Test
    void filtersAndDeduplicates() {
        PhantomClient client = mock(PhantomClient.class);
        AlumniRepository repo = mock(AlumniRepository.class);
        AlumniService service = new AlumniService(client, repo);

        SearchRequest request =
                new SearchRequest("VIT AP University", "Engineer", 2026);

        AlumniProfile alice = new AlumniProfile(
                "Alice", "Software Engineer", "VIT AP University",
                "India", "Engineer", 2026,
                "https://www.linkedin.com/in/alice");

        AlumniProfile bob = new AlumniProfile(
                "Bob", "Software Engineer", "Other University",
                "India", "Engineer", 2026,
                "https://www.linkedin.com/in/bob");

        when(client.search(request))
                .thenReturn(List.of(alice, alice, bob));

        assertEquals(List.of(alice), service.search(request));
        verify(repo).saveAll(List.of(alice));
    }

    @Test
    void excludesUnknownYearWhenYearIsRequested() {
        PhantomClient client = mock(PhantomClient.class);
        AlumniRepository repo = mock(AlumniRepository.class);
        AlumniService service = new AlumniService(client, repo);

        SearchRequest request =
                new SearchRequest("VIT AP University", "Engineer", 2026);

        AlumniProfile profile = new AlumniProfile(
                "Alice", "Engineer", "VIT AP University",
                null, null, null,
                "https://www.linkedin.com/in/alice");

        when(client.search(request)).thenReturn(List.of(profile));

        assertTrue(service.search(request).isEmpty());
    }

    @Test
    void providerFailureDoesNotSave() {
        PhantomClient client = mock(PhantomClient.class);
        AlumniRepository repo = mock(AlumniRepository.class);
        AlumniService service = new AlumniService(client, repo);

        SearchRequest request =
                new SearchRequest("VIT AP University", "Engineer", null);

        when(client.search(request))
                .thenThrow(new IllegalStateException("Provider failed"));

        assertThrows(
                IllegalStateException.class,
                () -> service.search(request));

        verifyNoInteractions(repo);
    }
}