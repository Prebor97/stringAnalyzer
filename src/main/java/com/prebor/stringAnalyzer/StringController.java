package com.prebor.stringAnalyzer;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/strings")
public class StringController {
    private final StringAnalysisService svc;

    public StringController(StringAnalysisService svc) {
        this.svc = svc;
    }

    // Create / Analyze String
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateStringRequest request) {
        Object val = request.getValue();
        if (val == null) return ResponseEntity.badRequest().body(Map.of("message", "value is required"));
        if (!(val instanceof String)) {
            // 422
            return ResponseEntity.unprocessableEntity().body(Map.of("message", "value must be a string"));
        }
        String raw = (String) val;
        if (raw.trim().isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "Invalid request body or missing value field"));
        // compute sha
        String sha = Sha256Util.sha256Hex(raw);
        if (svc.findByValue(raw).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "String already exists in the system"));
        }
        // compute and save
        StringEntity saved = svc.analyzeAndSave(raw);
        if (saved == null) { // in race case
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "String already exists in the system"));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    // Get specific string by its raw value (URL encoded)
    @GetMapping("/{stringValue}")
    public ResponseEntity<?> getByValue(@PathVariable("stringValue") String stringValue) {
        Optional<StringEntity> opt = svc.findByValue(stringValue);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "String not found"));
        }
        return ResponseEntity.ok(toDto(opt.get()));
    }

    // Delete
    @DeleteMapping("/{stringValue}")
    public ResponseEntity<?> deleteByValue(@PathVariable("stringValue") String stringValue) {
        boolean deleted = svc.deleteByValue(stringValue);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "String not found"));
        }
        return ResponseEntity.noContent().build();
    }

    // List with filters
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllWithFilters(
            @RequestParam(required = false) Boolean is_palindrome,
            @RequestParam(required = false) Integer min_length,
            @RequestParam(required = false) Integer max_length,
            @RequestParam(required = false) Integer word_count,
            @RequestParam(required = false, name = "contains_character") String contains_character
    ) {
        try {
            List<StringEntity> filtered = svc.filter(
                    is_palindrome,
                    min_length,
                    max_length,
                    word_count,
                    contains_character
            );

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("data", filtered);
            response.put("count", filtered.size());

            Map<String, Object> filtersApplied = new LinkedHashMap<>();
            if (is_palindrome != null) filtersApplied.put("is_palindrome", is_palindrome);
            if (min_length != null) filtersApplied.put("min_length", min_length);
            if (max_length != null) filtersApplied.put("max_length", max_length);
            if (word_count != null) filtersApplied.put("word_count", word_count);
            if (contains_character != null) filtersApplied.put("contains_character", contains_character);
            response.put("filters_applied", filtersApplied);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = Map.of("error", "Invalid query parameter values or types");
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Natural language filtering
    @GetMapping("/filter-by-natural-language")
    public ResponseEntity<?> filterByNaturalLanguage(@RequestParam("query") String query) {
        StringAnalysisService.NLParseResult parsed = svc.parseNaturalLanguageQuery(query);
        if (parsed == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Unable to parse natural language query"));
        }
        if (parsed.minLength != null && parsed.maxLength != null && parsed.minLength > parsed.maxLength) {
            return ResponseEntity.unprocessableEntity()
                    .body(Map.of("error", "Query parsed but resulted in conflicting filters"));
        }
        // check conflict (not much conflict detection in this simple version)
        // apply parsed filters
        List<StringEntity> matches = svc.filter(parsed.isPalindrome, parsed.minLength, parsed.maxLength, parsed.wordCount, parsed.containsCharacter);

        // build response
        List<Object> data = matches.stream().map(this::toDto).collect(Collectors.toList());
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("data", data);
        resp.put("count", data.size());
        Map<String, Object> parsedFilters = new LinkedHashMap<>();
        parsedFilters.put("original", parsed.original);
        Map<String, Object> pf = new LinkedHashMap<>();
        if (parsed.wordCount != null) pf.put("word_count", parsed.wordCount);
        if (parsed.isPalindrome != null) pf.put("is_palindrome", parsed.isPalindrome);
        if (parsed.minLength != null) pf.put("min_length", parsed.minLength);
        if (parsed.maxLength != null) pf.put("max_length", parsed.maxLength);
        if (parsed.containsCharacter != null) pf.put("contains_character", parsed.containsCharacter);
        resp.put("interpreted_query", Map.of(
                "original", parsed.original,
                "parsed_filters", pf
        ));

        return ResponseEntity.ok(resp);
    }

    // Helper to build DTO
    private StringResponseDto toDto(StringEntity e) {
        PropertiesDto p = new PropertiesDto();
        p.setLength(e.getLength());
        p.setIs_palindrome(e.isPalindrome());
        p.setUnique_characters(e.getUniqueCharacters());
        p.setWord_count(e.getWordCount());
        p.setSha256_hash(e.getId());
        p.setCharacter_frequency_map(e.getCharacterFrequencyMap());

        StringResponseDto dto = new StringResponseDto();
        dto.setId(e.getId());
        dto.setValue(e.getValue());
        dto.setProperties(p);
        dto.setCreated_at(e.getCreatedAt() == null ? Instant.now() : e.getCreatedAt());
        return dto;
    }
}
