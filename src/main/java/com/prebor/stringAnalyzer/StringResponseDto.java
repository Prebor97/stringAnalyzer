package com.prebor.stringAnalyzer;

import java.time.Instant;

public class StringResponseDto {
    private String id;
    private String value;
    private PropertiesDto properties;
    private Instant created_at;

    // getters & setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public PropertiesDto getProperties() { return properties; }
    public void setProperties(PropertiesDto properties) { this.properties = properties; }
    public Instant getCreated_at() { return created_at; }
    public void setCreated_at(Instant created_at) { this.created_at = created_at; }
}
