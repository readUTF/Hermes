package com.github.readutf.hermes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.readutf.hermes.listeners.ListenerHandler;
import com.github.readutf.hermes.serialization.SerializationManager;
import com.github.readutf.hermes.serialization.StringSerializer;
import com.github.readutf.hermes.wrapper.ParcelResponse;
import com.github.readutf.hermes.wrapper.ParcelWrapper;
import com.readutf.uls.Logger;
import com.readutf.uls.LoggerFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

@Getter
public class Hermes {

    private @Getter
    static Hermes instance;
    private @Getter
    @Setter LoggerFactory loggerFactory;

    private final Logger logger;
    private final JedisPool jedisPool;
    private final Jedis publisher;
    private final ObjectMapper objectMapper;
    private final ExecutorService threadPool;
    private final ListenerHandler listenerHandler;
    private final SerializationManager serializationManager;

    private HermesSubscriber pubSub;

    boolean running = true;

    private final HashMap<UUID, Consumer<ParcelResponse>> responseConsumerMap;
    private final HashMap<String, TypeReference<?>> typeAdapters = new HashMap<>();

    @SneakyThrows
    private Hermes(LoggerFactory loggerFactory, JedisPool jedisPool, ObjectMapper objectMapper, ClassLoader classLoader) {
        instance = this;
        this.loggerFactory = loggerFactory;

        new Thread(() -> {
            try {
                Jedis jedis = jedisPool.borrowObject();
                jedis.psubscribe(pubSub = new HermesSubscriber(this), "*");
                jedisPool.returnObject(jedis);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        this.jedisPool = jedisPool;
        this.logger = loggerFactory.getLogger(getClass());
        this.objectMapper = objectMapper;
        this.publisher = jedisPool.getResource();
        this.serializationManager = new SerializationManager();
        this.responseConsumerMap = new HashMap<>();
        this.threadPool = Executors.newCachedThreadPool();
        this.listenerHandler = new ListenerHandler(this, classLoader);
    }


    @SneakyThrows
    public void sendParcel(String channel, Object object, Consumer<ParcelResponse> responseConsumer) {
        UUID parcelId = UUID.randomUUID();

        StringSerializer<Object> custom = (StringSerializer<Object>) serializationManager.getStringSerializer(channel);
        String message;
        if(custom != null) {
            message = 0 + custom.serialize(object);
        } else {
            message = 1 + Base64.getEncoder().encodeToString(objectMapper.writeValueAsString(new ParcelWrapper(channel, parcelId, object)).getBytes());
        }
        try {
            publisher.publish(channel, message);
            if (responseConsumer != null) responseConsumerMap.put(parcelId, responseConsumer);
        } catch (Exception e) {
            System.out.println("Error on send: " + e.getMessage());
        }
    }

    @SneakyThrows
    public void sendParcel(String channel, Object object) {
        sendParcel(channel, object, null);
    }

    public void sendResponse(String channel, UUID parcelId, ParcelResponse parcelResponse) {
        try {
            parcelResponse.setParcelId(parcelId);
            publisher.publish(channel, "1" + Base64.getEncoder().encodeToString(objectMapper.writeValueAsString(parcelResponse).getBytes()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void registerListener(Class<?> listener, Object... props) {
        listenerHandler.registerChannelListener(listener, props);
    }

    public void registerListener(Object instance) {
        listenerHandler.registerChannelListener(instance);
    }

    public void close() {
        publisher.close();
        running = false;
    }

    public static class Builder {

        private LoggerFactory loggerFactory = new LoggerFactory("Hermes");
        private JedisPool jedisPool;
        private ObjectMapper objectMapper = new ObjectMapper();
        private ClassLoader classLoader = Hermes.class.getClassLoader();

        public Hermes build() {
            if(jedisPool == null) throw new RuntimeException("JedisPool cannot be null");
            return new Hermes(loggerFactory, jedisPool, objectMapper, classLoader);
        }

        public Builder loggerFactory(LoggerFactory loggerFactory) {
            this.loggerFactory = loggerFactory;
            return this;
        }

        public Builder jedisPool(JedisPool jedisPool) {
            this.jedisPool = jedisPool;
            return this;
        }

        public Builder objectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        public Builder classLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

    }

}
