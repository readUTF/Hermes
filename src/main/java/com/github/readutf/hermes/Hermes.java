package com.github.readutf.hermes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.readutf.hermes.pipline.ParcelConsumer;
import com.github.readutf.hermes.pipline.listeners.ParcelListenerManager;
import com.github.readutf.hermes.senders.ParcelSender;
import com.github.readutf.hermes.serializer.StringSerializer;
import com.github.readutf.hermes.subscribers.ParcelSubscriber;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Hermes {

    private static final Logger logger = LoggerFactory.getLogger(Hermes.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final @Getter String prefix;
    private final Thread subscriberThread;
    private final ParcelConsumer parcelConsumer;
    private final ParcelListenerManager parcelListenerManager;
    private final ParcelSender parcelSender;

    private Hermes(String prefix, ParcelSubscriber parcelSubscriber, ParcelSender parcelSender) {
        logger.info("Hermes is starting up... (prefix: {})", prefix);
        this.prefix = prefix;
        this.parcelListenerManager = new ParcelListenerManager();
        this.parcelConsumer = new ParcelConsumer(parcelListenerManager);
        this.parcelSender = parcelSender;
        subscriberThread = new Thread(() -> {
            parcelSubscriber.subscribe(this, parcelConsumer);
        });
    }

    public <T> void sendParcel(String channel, T object, StringSerializer<T> serializer) {
        try {
            String serialize = serializer.serialize(object);
            parcelSender.send(channel, serialize);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendParcel(String channel, Object object) {
        try {
            parcelSender.send(channel, objectMapper.writeValueAsString(object));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void connect() {
        logger.info("Hermes is connecting... (prefix: {})", prefix);

        subscriberThread.start();
    }

    public void disconnect() {

    }

    public static class Builder {

        private String prefix;
        private ParcelSubscriber parcelSubscriber;
        private ParcelSender parcelSender;
        private List<Object> listeners = new ArrayList<>();

        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder addListener(Object... listeners) {
            this.listeners.addAll(Arrays.asList(listeners));
            return this;
        }

        public Builder parcelSubscriber(ParcelSubscriber parcelSubscriber) {
            this.parcelSubscriber = parcelSubscriber;
            return this;
        }

        public Builder parcelSender(ParcelSender parcelSender) {
            this.parcelSender = parcelSender;
            return this;
        }

        public Hermes build() {
            if(prefix == null) throw new NullPointerException("Prefix is null");
            if(parcelSubscriber == null) throw new NullPointerException("ParcelSubscriber is null");
            if(parcelSender == null) throw new NullPointerException("ParcelSender is null");
            Hermes hermes = new Hermes(prefix, parcelSubscriber, parcelSender);
            hermes.parcelListenerManager.registerListeners(listeners.toArray());
            return hermes;
        }
    }

}
