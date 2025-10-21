package com.prebor.stringAnalyzer;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "strings")
public class StringEntity {
    @Id
    private String id; // sha256

    @Column(name = "string_value", nullable = false)
    private String value;

    private int length;

    private boolean isPalindrome;

    private int uniqueCharacters;

    private int wordCount;

    private Instant createdAt;

    // store character frequency as map<string->int> (character as single-string key)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "character_frequency_map", joinColumns = @JoinColumn(name = "string_id"))
    @MapKeyColumn(name = "character_key")
    @Column(name = "occurrence")
    private Map<String, Integer> characterFrequencyMap;

    public StringEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public int getLength() { return length; }
    public void setLength(int length) { this.length = length; }
    public boolean isPalindrome() { return isPalindrome; }
    public void setPalindrome(boolean palindrome) { isPalindrome = palindrome; }
    public int getUniqueCharacters() { return uniqueCharacters; }
    public void setUniqueCharacters(int uniqueCharacters) { this.uniqueCharacters = uniqueCharacters; }
    public int getWordCount() { return wordCount; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Map<String, Integer> getCharacterFrequencyMap() { return characterFrequencyMap; }
    public void setCharacterFrequencyMap(Map<String, Integer> characterFrequencyMap) { this.characterFrequencyMap = characterFrequencyMap; }
}