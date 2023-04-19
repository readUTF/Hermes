package com.github.readutf.hermes.wrapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.readutf.hermes.Hermes;
import com.github.readutf.hermes.serializer.StringSerializer;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParcelWrapper {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(ParcelWrapper.class);

    private @Getter final String channel;
    private @Getter final String rawData;

    public ParcelWrapper(String channel, String rawData) {
        this.channel = channel;
        this.rawData = rawData;
    }

    public <T> T get(@NonNull Class<T> clazz) {
        logger.debug("Converting {} to {}", rawData, clazz.getSimpleName());
        return objectMapper.convertValue(rawData, clazz);
    }

    public <T> T get(@NonNull StringSerializer<T> serializer) throws Exception {
        logger.debug("Deserializing {} with {}", rawData, serializer.getClass());
        return serializer.deserialize(rawData);
    }

    @SneakyThrows
    public <T> T get(@NonNull TypeReference<T> typeReference) {
        logger.info("Converting {} to {}", rawData, typeReference.getType().getTypeName());
//        System.out.println("Converting " + rawData + " to " + typeReference.getType().getTypeName());
        return objectMapper.readValue(rawData, typeReference);
    }

    public String getAsString() {
        return rawData;
    }

}
