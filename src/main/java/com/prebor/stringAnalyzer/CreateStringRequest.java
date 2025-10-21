package com.prebor.stringAnalyzer;

import jakarta.validation.constraints.NotNull;

public class CreateStringRequest {
    @NotNull(message = "\"value\" is required")
    private Object value;

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
