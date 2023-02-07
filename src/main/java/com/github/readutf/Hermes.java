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
    private @Getter @Setter static LoggerFactory loggerFactory = new LoggerFactory("Hermes").setDebugAll(true);

    private Logger logger;
    private JedisPool jedisPool;
    private Jedis publisher;
    private Jedis subscriber;
    private ObjectMapper objectMapper;
    private ExecutorService threadPool;
    private ListenerHandler listenerHandler;

    private List<HermesSubscriber> subscribers;

    List<String> registeredChannels = new ArrayList<>();

    boolean running = true;

    private HashMap<UUID, Consumer<ParcelResponse>> responseConsumerMap;
    private HashMap<String, List<Function<ParcelWrapper, ParcelResponse>>> parcelConsumerMap;
    private HashMap<String, TypeReference<?>> typeAdapters = new HashMap<>();

    public Hermes(JedisPool jedisPool, ObjectMapper objectMapper, ClassLoader classLoader) {
        instance = this;

        this.jedisPool = jedisPool;
        this.logger = loggerFactory.getLogger(getClass());
        this.objectMapper = objectMapper;
        this.publisher = jedisPool.getResource();
        this.subscriber = jedisPool.getResource();
        this.responseConsumerMap = new HashMap<>();
        this.parcelConsumerMap = new HashMap<>();
        this.threadPool = Executors.newCachedThreadPool();
        this.listenerHandler = new ListenerHandler(this, classLoader);
        this.subscribers = new ArrayList<>();
    }

    public Hermes(JedisPool jedisPool) {
        this(jedisPool, new ObjectMapper(), Hermes.class.getClassLoader());
    }

    public void registerResponseReceiver(String channel, Consumer<ParcelResponse> responseConsumer) {
        responseConsumerMap.put(UUID.randomUUID(), responseConsumer);
        registerChannel(channel);
    }

    public void registerReceiver(String channel, Function<ParcelWrapper, ParcelResponse> parcelConsumer) {
        List<Function<ParcelWrapper, ParcelResponse>> consumers = parcelConsumerMap.getOrDefault(channel, new ArrayList<>());
        consumers.add(parcelConsumer);
        parcelConsumerMap.put(channel, consumers);
        registerChannel(channel);
    }

    @SneakyThrows
    public void sendParcel(String channel, Object object, Consumer<ParcelResponse> responseConsumer) {
        UUID parcelId = UUID.randomUUID();
        publisher.publish(channel, objectMapper.writeValueAsString(new ParcelWrapper(parcelId, object)));
        responseConsumerMap.put(parcelId, responseConsumer);
        registerChannel(channel);
    }

    @SneakyThrows
    public void sendParcel(String channel, Object object) {
        UUID parcelId = UUID.randomUUID();
        String message = objectMapper.writeValueAsString(new ParcelWrapper(parcelId, object));
        publisher.publish(channel, message);
        registerChannel(channel);
    }

    public void sendResponse(String channel, UUID parcelId, ParcelResponse parcelResponse) {
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

    public void registerChannel(String channel) {
        logger.debug("Registering channel: " + channel);
        if (registeredChannels.contains(channel)) return;
        threadPool.submit(() -> {
            try {
                HermesSubscriber subscriber1 = new HermesSubscriber(this);
                subscribers.add(subscriber1);
                Jedis jedis = jedisPool.borrowObject();
                jedis.subscribe(subscriber1, channel);
                jedisPool.returnObject(jedis);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void close() {
        publisher.close();
        subscriber.close();
        running = false;
    }


}
