package com.github.readutf.wrapper;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor @NoArgsConstructor
@Getter @Setter @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class ParcelWrapper {

    String subChannel;
    UUID parcelId;
    Object data;

    @Override
    public String toString() {
        return "ParcelWrapper{" +
                "parcelId=" + parcelId +
                ", data=" + data +
                '}';
    }
}
