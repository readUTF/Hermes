package com.github.readutf.hermes.serialization;

import java.util.HashMap;
import java.util.Map;

public class SerializationManager {

    private final Map<String, StringSerializer<?>> stringSerializerMap;

    public SerializationManager() {
        stringSerializerMap = new HashMap<>();
    }

    public void register(String channelName, StringSerializer<?> serializer) {
        stringSerializerMap.put(channelName, serializer);
    }

    public StringSerializer<?> getStringSerializer(String channelName) {
        return stringSerializerMap.get(channelName);
    }



}
