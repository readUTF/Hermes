package com.github.readutf.hermes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.readutf.hermes.serialization.SerializationManager;
import com.github.readutf.hermes.serialization.StringSerializer;
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
    private final SerializationManager serializationManager;

    public HermesSubscriber(Hermes hermes) {
        this.hermes = hermes;
        this.logger = hermes.getLoggerFactory().getLogger(getClass());
        this.serializationManager = hermes.getSerializationManager();
    }

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {
        System.out.println("Channel " + channel + " registered.");
    }


    @Override
    public void onPMessage(String pattern, String channel, String message) {
        Integer parcelDataType = Integer.parseInt(message.substring(0, 1));

        if (parcelDataType == 0) {
            StringSerializer<?> stringSerializer = serializationManager.getStringSerializer(channel);
            if (stringSerializer != null) {
                Object deserialized = null;
                try {
                    deserialized = stringSerializer.deserialize(message.substring(1));
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
                hermes.getListenerHandler().handleParcel(channel, null, deserialized);
            }
        } else if (parcelDataType == 1) {
            message = message.substring(1);


            String messageDecoded = new String(Base64.getDecoder().decode(message));

            logger.debug("Received parcel data");
            threadPool.submit(() -> {
                ObjectMapper objectMapper = hermes.getObjectMapper();
                try {
                    HashMap<String, Object> jsonNode = objectMapper.readValue(messageDecoded, new TypeReference<HashMap<String, Object>>() {
                    });
                    String type = (String) jsonNode.get("@class");

                    if (type.contains("ParcelWrapper")) {
                        logger.debug("Processing ParcelWrapper");

                        ParcelWrapper parcelWrapper = objectMapper.convertValue(jsonNode, ParcelWrapper.class);
                        try {
                            hermes.getListenerHandler().handleParcel(channel, parcelWrapper.getParcelId(), parcelWrapper.getData());
                        } catch (Exception e) {
                            e.printStackTrace();
                            logger.debug("Failed to handle parcel");
                            return;
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
}
