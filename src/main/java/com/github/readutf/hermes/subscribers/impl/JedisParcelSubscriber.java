package com.github.readutf.hermes.subscribers.impl;

import com.github.readutf.hermes.Hermes;
import com.github.readutf.hermes.pipline.ParcelConsumer;
import com.github.readutf.hermes.subscribers.ParcelSubscriber;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;

public class JedisParcelSubscriber extends JedisPubSub implements ParcelSubscriber {

    private final JedisPool jedisPool;
    private Hermes hermes;
    private ParcelConsumer consumer;

    public JedisParcelSubscriber(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @Override
    public void onPMessage(String pattern, String channel, String message) {
        if(hermes == null) throw new NullPointerException("Hermes is null");

        consumer.consumeParcel(channel, message);
    }

    @Override
    public void subscribe(Hermes hermes, ParcelConsumer consumer) {
        this.consumer = consumer;
        this.hermes = hermes;
        jedisPool.getResource().psubscribe(this, hermes.getPrefix() + "*");
    }
}
