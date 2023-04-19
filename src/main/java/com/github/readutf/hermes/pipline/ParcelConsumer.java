package com.github.readutf.hermes.pipline;

import com.github.readutf.hermes.pipline.listeners.ParcelListenerManager;

public class ParcelConsumer {

    ParcelListenerManager parcelListenerManager;

    public ParcelConsumer(ParcelListenerManager parcelListenerManager) {
        this.parcelListenerManager = parcelListenerManager;
    }

    public void consume(String channel, String message) {
        parcelListenerManager.call(channel, message);
    }

}
