package com.github.readutf.hermes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.readutf.hermes.inbuilt.PingCalculator;
import com.github.readutf.hermes.pipline.ParcelConsumer;
import com.github.readutf.hermes.pipline.listeners.ParcelListenerManager;
import com.github.readutf.hermes.senders.ParcelSender;
import com.github.readutf.hermes.serializer.StringSerializer;
import com.github.readutf.hermes.subscribers.ParcelSubscriber;
import com.github.readutf.hermes.wrapper.ParcelWrapper;
import lombok.Getter;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;


public class Hermes {

    private static final Logger logger = LoggerFactory.getLogger(Hermes.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final @Getter String prefix;
    private final Thread subscriberThread;
    private final ParcelConsumer parcelConsumer;
    private @Getter final ParcelListenerManager parcelListenerManager;
    private final PingCalculator pingCalculator;
    private final ParcelSender parcelSender;
    private final ExecutorService senderPool = Executors.newSingleThreadExecutor();

    private final Map<UUID, Consumer<ParcelWrapper>> responseHandlers = new HashMap<>();

    private Hermes(String prefix, ParcelSubscriber parcelSubscriber, ParcelSender parcelSender) {
        logger.info("Hermes is starting up... (prefix: {})", prefix);
        this.prefix = prefix;
        this.parcelListenerManager = new ParcelListenerManager(this);
        this.parcelConsumer = new ParcelConsumer(this);
        this.parcelSender = parcelSender;
        this.pingCalculator = new PingCalculator(this);
        (subscriberThread = new Thread(() -> {
            parcelSubscriber.subscribe(this, parcelConsumer);
        })).start();

    }

    @SneakyThrows
    public <T> void sendParcel(String channel, T object, StringSerializer<T> serializer) {
        sendParcel(channel, UUID.randomUUID(), serializer.serialize(object), null);
    }

    @SneakyThrows
    public <T> void sendParcel(String channel, T object, StringSerializer<T> serializer, Consumer<ParcelWrapper> response) {
        sendParcel(channel, serializer.serialize(object), response);
    }

    @SneakyThrows
    public void sendParcel(String channel, Object object) {
        sendParcel(channel, UUID.randomUUID(), objectMapper.writeValueAsString(object), null);
    }

    @SneakyThrows
    public void sendParcel(String channel, Object object, Consumer<ParcelWrapper> response) {
        sendParcel(channel, UUID.randomUUID(), objectMapper.writeValueAsString(object), response);
    }

    private void sendParcel(String channel, UUID parcelId, String data, Consumer<ParcelWrapper> responseHandler) {
        try {
            senderPool.submit(() -> {
                parcelSender.send(prefix + "_" + channel, parcelId.toString() + ";" + data);
                if(responseHandler != null) {
                    responseHandlers.put(parcelId, responseHandler);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public <T> CompletableFuture<ParcelWrapper> sendParcelFuture(String channel, T object, StringSerializer<T> serializer) {
        CompletableFuture<ParcelWrapper> future = new CompletableFuture<>();
        sendParcel(channel, object, serializer, parcelWrapper -> {
            try {
                future.complete(parcelWrapper);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public <T> CompletableFuture<ParcelWrapper> sendParcelFuture(String channel, T object) {
        CompletableFuture<ParcelWrapper> future = new CompletableFuture<>();
        sendParcel(channel, object, parcelWrapper -> {
            try {
                future.complete(parcelWrapper);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public void addParcelListener(Object object) {
        parcelListenerManager.registerListeners(object);
    }

    public long ping(boolean debug) {
        return pingCalculator.calculatePing(debug);
    }

    public Consumer<ParcelWrapper> getResponseHandler(UUID parcelId) {
        return responseHandlers.get(parcelId);
    }

    @SneakyThrows
    public void sendResponse(UUID parcelId, Object object) {
        parcelSender.send(prefix + "_RESPONSE", parcelId + ";" + objectMapper.writeValueAsString(object));
    }

    public void connect() {

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

    public static Builder builder() {
        return new Builder();
    }

}
