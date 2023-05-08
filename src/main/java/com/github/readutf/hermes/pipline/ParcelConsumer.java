package com.github.readutf.hermes.pipline;

import com.github.readutf.hermes.Hermes;
import com.github.readutf.hermes.pipline.listeners.ParcelListenerManager;
import com.github.readutf.hermes.wrapper.ParcelWrapper;

import java.util.UUID;

public class ParcelConsumer {

    private final Hermes hermes;
    private final ParcelListenerManager parcelListenerManager;

    public ParcelConsumer(Hermes hermes) {
        this.hermes = hermes;
        this.parcelListenerManager = hermes.getParcelListenerManager();
    }

    public void consumeParcel(String channel, String rawData) {
        int splitIndex = rawData.indexOf(";");
        UUID parcelId = UUID.fromString(rawData.substring(0, splitIndex));
        String message = rawData.substring(splitIndex+1);

        if(channel.equalsIgnoreCase(hermes.getPrefix() + "_RESPONSE")) {
            consumeResponse(parcelId, message);
            return;
        }

        parcelListenerManager.call(channel, parcelId, message);
    }

    public void consumeResponse(UUID parcelId, String responseData) {
        hermes.getResponseHandler(parcelId).accept(new ParcelWrapper("response", responseData));
    }


}
