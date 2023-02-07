package com.github.readutf;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.readutf.wrapper.ParcelResponse;
import com.github.readutf.wrapper.ParcelWrapper;
import com.readutf.uls.Logger;
import redis.clients.jedis.JedisPubSub;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

public class HermesSubscriber extends JedisPubSub {

    ExecutorService threadPool = Executors.newCachedThreadPool();

    private static Logger logger = Hermes.getLoggerFactory().getLogger(HermesSubscriber.class);

    Hermes hermes;

    public HermesSubscriber(Hermes hermes) {
        this.hermes = hermes;
    }

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {
        logger.debug("subbed to: " + channel);
    }

    @Override
    public void onMessage(String channel, String message) {
        logger.debug("Received parcel data in " + channel);
        threadPool.submit(() -> {
            ObjectMapper objectMapper = hermes.getObjectMapper();
            try {
                HashMap<String, Object> jsonNode = objectMapper.readValue(message, new TypeReference<HashMap<String, Object>>() {});
                String type = (String) jsonNode.get("@class");

                if (type.contains("ParcelWrapper")) {
                    logger.debug("Processing ParcelWrapper");

                    ParcelWrapper parcelWrapper = objectMapper.convertValue(jsonNode, ParcelWrapper.class);
                    try {
                        hermes.getListenerHandler().handleParcel(channel, parcelWrapper);
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.debug("Failed to handle parcel");
                        return;
                    }
                    List<Function<ParcelWrapper, ParcelResponse>> functions = hermes.getParcelConsumerMap().get(channel);
                    if(functions == null) {
                        logger.debug("No parcel handlers found for channel: " + channel);
                        return;
                    }
                    for (Function<ParcelWrapper, ParcelResponse> function : functions) {
                        ParcelResponse parcelResponse = function.apply(parcelWrapper);
                        hermes.sendResponse(channel, parcelWrapper.getParcelId(), parcelResponse);
                    }
                } else if(type.contains("ParcelResponse")) {
                    ParcelResponse parcelResponse = objectMapper.convertValue(jsonNode, ParcelResponse.class);
                    Consumer<ParcelResponse> parcelResponseConsumer = hermes.getResponseConsumerMap().get(parcelResponse.getParcelId());
                    if (parcelResponseConsumer == null) return;
                    parcelResponseConsumer.accept(parcelResponse);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
