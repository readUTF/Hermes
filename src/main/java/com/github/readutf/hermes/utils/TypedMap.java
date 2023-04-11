package com.github.readutf.hermes.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class TypedMap extends HashMap<String, Object> {

    private static ObjectMapper objectMapper = new ObjectMapper();

    public <T> T get(Object key, TypeReference<T> typeReference) {
        return objectMapper.convertValue(get(key), typeReference);
    }
}
