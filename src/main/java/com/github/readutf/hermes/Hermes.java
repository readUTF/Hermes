package com.github.readutf.hermes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.readutf.hermes.listeners.ListenerHandler;
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

    private HermesSubscriber pubSub;

    boolean running = true;

    private final HashMap<UUID, Consumer<ParcelResponse>> responseConsumerMap;
    private final HashMap<String, List<Function<ParcelWrapper, ParcelResponse>>> parcelConsumerMap;
    private final HashMap<String, TypeReference<?>> typeAdapters = new HashMap<>();

    private final HashMap<String, Runnable> awaitingSubscribe = new HashMap<>();

    String channel;

    @SneakyThrows
    private Hermes(String channelName, LoggerFactory loggerFactory, JedisPool jedisPool, ObjectMapper objectMapper, ClassLoader classLoader) {
        instance = this;
        this.channel = channelName;
        this.loggerFactory = loggerFactory;

        new Thread(() -> {
            try {
                Jedis jedis = jedisPool.borrowObject();
                jedis.subscribe(pubSub = new HermesSubscriber(this), channelName);
                jedisPool.returnObject(jedis);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        this.jedisPool = jedisPool;
        this.logger = loggerFactory.getLogger(getClass());
        this.objectMapper = objectMapper;
        this.publisher = jedisPool.getResource();
        this.responseConsumerMap = new HashMap<>();
        this.parcelConsumerMap = new HashMap<>();
        this.threadPool = Executors.newCachedThreadPool();
        this.listenerHandler = new ListenerHandler(this, classLoader);
    }

    public void registerResponseReceiver(String channel, Consumer<ParcelResponse> responseConsumer) {
        responseConsumerMap.put(UUID.randomUUID(), responseConsumer);
    }

    public void registerReceiver(String channel, Function<ParcelWrapper, ParcelResponse> parcelConsumer) {
        List<Function<ParcelWrapper, ParcelResponse>> consumers = parcelConsumerMap.getOrDefault(channel, new ArrayList<>());
        consumers.add(parcelConsumer);
        parcelConsumerMap.put(channel, consumers);
    }

    @SneakyThrows
    public void sendParcel(String subChannel, Object object, Consumer<ParcelResponse> responseConsumer) {
        UUID parcelId = UUID.randomUUID();
        String message = objectMapper.writeValueAsString(new ParcelWrapper(subChannel, parcelId, object));
        message = Base64.getEncoder().encodeToString(message.getBytes());
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

    public void sendResponse(String subChannel, UUID parcelId, ParcelResponse parcelResponse) {
        try {
            parcelResponse.setChannel(channel);
            parcelResponse.setParcelId(parcelId);
            publisher.publish(channel, Base64.getEncoder().encodeToString(objectMapper.writeValueAsString(parcelResponse).getBytes()));
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

        private String channel;
        private LoggerFactory loggerFactory = new LoggerFactory("Hermes");
        private JedisPool jedisPool;
        private ObjectMapper objectMapper = new ObjectMapper();
        private ClassLoader classLoader = Hermes.class.getClassLoader();

        public Hermes build() {
            if(channel == null) throw new RuntimeException("Channel cannot be null");
            if(jedisPool == null) throw new RuntimeException("JedisPool cannot be null");
            return new Hermes(channel, loggerFactory, jedisPool, objectMapper, classLoader);
        }

        public Builder channel(String channel) {
            this.channel = channel;
            return this;
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
