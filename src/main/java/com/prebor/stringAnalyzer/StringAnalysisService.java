package com.prebor.stringAnalyzer;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StringAnalysisService {
    private final StringRepository repository;

    public StringAnalysisService(StringRepository repository) {
        this.repository = repository;
    }

    public StringEntity analyzeAndSave(String rawValue) {
        String sha = Sha256Util.sha256Hex(rawValue);
        if (repository.existsById(sha)) {
            return null; // caller will treat as conflict
        }

        StringEntity entity = computeProperties(rawValue);
        entity.setId(sha);
        entity.setCreatedAt(Instant.now());
        entity.setValue(rawValue);

        repository.save(entity);
        return entity;
    }

    public StringEntity computeProperties(String rawValue) {
        StringEntity e = new StringEntity();
        e.setValue(rawValue);

        // Normalize string for consistent analysis
        String trimmed = rawValue.trim();

        // Remove all spaces for length and frequency calculations
        String noSpaceValue = trimmed.replaceAll("\\s+", "");

        //  Length excluding spaces
        e.setLength(noSpaceValue.length());

        // ✅ Word count (split by whitespace)
        String[] words = trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+");
        e.setWordCount(words.length);

        // ✅ Character frequency map (excluding spaces)
        Map<String, Integer> freq = new LinkedHashMap<>();
        for (char ch : noSpaceValue.toCharArray()) {
            String c = String.valueOf(ch);
            freq.put(c, freq.getOrDefault(c, 0) + 1);
        }
        e.setCharacterFrequencyMap(freq);

        // ✅ Unique characters (excluding spaces)
        e.setUniqueCharacters(freq.size());

        // ✅ Is palindrome (ignore spaces, case-insensitive)
        String normalized = noSpaceValue.toLowerCase(Locale.ROOT);
        String reversed = new StringBuilder(normalized).reverse().toString();
        e.setPalindrome(normalized.equals(reversed));

        return e;
    }

    public Optional<StringEntity> findByValue(String rawValue) {
        String sha = Sha256Util.sha256Hex(rawValue);
        return repository.findById(sha);
    }

    public List<StringEntity> listAll() {
        return repository.findAll();
    }

    public boolean deleteByValue(String rawValue) {
        String sha = Sha256Util.sha256Hex(rawValue);
        if (repository.existsById(sha)) {
            repository.deleteById(sha);
            return true;
        }
        return false;
    }

    // ✅ Filtering in-memory using optional parameters
    public List<StringEntity> filter(Boolean isPalindrome,
                                     Integer minLength,
                                     Integer maxLength,
                                     Integer wordCount,
                                     String containsCharacter) {
        return repository.findAll().stream()
                .filter(e -> isPalindrome == null || e.isPalindrome() == isPalindrome)
                .filter(e -> minLength == null || e.getLength() >= minLength)
                .filter(e -> maxLength == null || e.getLength() <= maxLength)
                .filter(e -> wordCount == null || e.getWordCount() == wordCount)
                .filter(e -> {
                    if (containsCharacter == null || containsCharacter.isEmpty()) return true;
                    String target = containsCharacter.toLowerCase(Locale.ROOT);
                    return e.getValue().toLowerCase(Locale.ROOT).contains(target);
                })
                .collect(Collectors.toList());
    }

    // ✅ Natural language parsing → heuristics
    public static class NLParseResult {
        public Boolean isPalindrome;
        public Integer minLength;
        public Integer maxLength;
        public Integer wordCount;
        public String containsCharacter;
        public String original;
    }

    public NLParseResult parseNaturalLanguageQuery(String query) {
        if (query == null || query.trim().isEmpty()) return null;
        String q = query.toLowerCase(Locale.ROOT).trim();
        NLParseResult r = new NLParseResult();
        r.original = query;

        // Examples heuristics:
        // "all single word palindromic strings" => word_count=1, is_palindrome=true
        if (q.contains("palind") || q.contains("palindrom")) {
            r.isPalindrome = true;
        }
        if (q.matches(".*\\bsingle word\\b.*") || q.matches(".*\\bone[- ]word\\b.*")) {
            r.wordCount = 1;
        }
        // "strings longer than 10 characters" -> min_length = 11
        java.util.regex.Matcher longer = java.util.regex.Pattern.compile("longer than (\\d+)").matcher(q);
        if (longer.find()) {
            int n = Integer.parseInt(longer.group(1));
            r.minLength = n + 1;
        }
        // "strings shorter than 5 characters" -> max_length = 4
        java.util.regex.Matcher shorter = java.util.regex.Pattern.compile("shorter than (\\d+)").matcher(q);
        if (shorter.find()) {
            int n = Integer.parseInt(shorter.group(1));
            r.maxLength = n - 1;
        }
        // "containing the letter z" or "strings containing z" -> contains_character=z
        java.util.regex.Matcher containsChar = java.util.regex.Pattern.compile("(?:containing|contain|that contain|contains|with the letter|that contain the letter)\\s+([a-z0-9])").matcher(q);
        if (containsChar.find()) {
            r.containsCharacter = containsChar.group(1);
        } else {
            // try single-letter mention "containing z"
            java.util.regex.Matcher singleChar = java.util.regex.Pattern.compile("containing\\s+([a-z0-9])\\b").matcher(q);
            if (singleChar.find()) r.containsCharacter = singleChar.group(1);
        }

        boolean any = r.isPalindrome != null || r.minLength != null || r.maxLength != null || r.wordCount != null || r.containsCharacter != null;
        return any ? r : null;
    }
}