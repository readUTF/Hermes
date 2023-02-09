package com.github.readutf;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.readutf.listeners.ListenerHandler;
import com.github.readutf.wrapper.ParcelResponse;
import com.github.readutf.wrapper.ParcelWrapper;
import com.readutf.uls.Logger;
import com.readutf.uls.LoggerFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

@Getter
public class Hermes {

    private @Getter
    static Hermes instance;
    private @Getter
    @Setter
    static LoggerFactory loggerFactory = new LoggerFactory("Hermes").setDebugAll(true);

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

    public Hermes(String channelName, JedisPool jedisPool, ObjectMapper objectMapper, ClassLoader classLoader) {
        instance = this;
        this.channel = channelName;

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

    public Hermes(String channelName, JedisPool jedisPool) {
        this(channelName, jedisPool, new ObjectMapper(), Hermes.class.getClassLoader());
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
        publisher.publish(channel, objectMapper.writeValueAsString(new ParcelWrapper(subChannel, parcelId, object)));
        if (responseConsumer != null) responseConsumerMap.put(parcelId, responseConsumer);


    }

    @SneakyThrows
    public void sendParcel(String channel, Object object) {
        sendParcel(channel, object, null);
    }

    public void sendResponse(String subChannel, UUID parcelId, ParcelResponse parcelResponse) {
        try {
            parcelResponse.setChannel(channel);
            parcelResponse.setParcelId(parcelId);
            publisher.publish(channel, objectMapper.writeValueAsString(parcelResponse));
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


}
