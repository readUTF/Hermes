package com.github.readutf.hermes.wrapper;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.readutf.hermes.Hermes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class") @Getter @NoArgsConstructor @Setter
public class ParcelResponse {

    private String channel;
    private UUID parcelId;

    private HashMap<String, Object> data;
    private ParcelResponseType responseType;

    public ParcelResponse(ParcelResponseType responseType, HashMap<String, Object> data) {
        this.data = data;
        this.responseType = responseType;
    }

    public <T> T getData(TypeReference<T> typeReference) {
        return Hermes.getInstance().getObjectMapper().convertValue(data, typeReference);
    }

    public enum ParcelResponseType {
        SUCCESS, ERROR, TIMEOUT
    }

}
