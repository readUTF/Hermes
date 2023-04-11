package com.github.readutf.hermes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.readutf.hermes.wrapper.ParcelResponse;
import com.github.readutf.hermes.wrapper.ParcelWrapper;
import com.readutf.uls.Logger;
import redis.clients.jedis.JedisPubSub;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

public class HermesSubscriber extends JedisPubSub {

    ExecutorService threadPool = Executors.newCachedThreadPool();

    private final Hermes hermes;
    private final Logger logger;

    public HermesSubscriber(Hermes hermes) {
        this.hermes = hermes;
        this.logger = hermes.getLoggerFactory().getLogger(getClass());
    }

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {
        System.out.println("Channel " + channel + " registered.");
    }

    @Override
    public void onPMessage(String pattern, String channel, String message) {
        String messageDecoded = new String(Base64.getDecoder().decode(message));

        logger.debug("Received parcel data");
        threadPool.submit(() -> {
            ObjectMapper objectMapper = hermes.getObjectMapper();
            try {
                HashMap<String, Object> jsonNode = objectMapper.readValue(messageDecoded, new TypeReference<HashMap<String, Object>>() {});
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
                    if (functions == null) return;

                    for (Function<ParcelWrapper, ParcelResponse> function : functions) {
                        ParcelResponse parcelResponse = function.apply(parcelWrapper);
                        hermes.sendResponse(channel, parcelWrapper.getParcelId(), parcelResponse);
                    }
                } else if (type.contains("ParcelResponse")) {
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
