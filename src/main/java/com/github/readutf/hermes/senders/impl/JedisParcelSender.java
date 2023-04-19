package com.github.readutf.hermes.senders.impl;

import com.github.readutf.hermes.senders.ParcelSender;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class JedisParcelSender implements ParcelSender {

    JedisPool jedisPool;

    public JedisParcelSender(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @Override
    public void send(String channel, String message) {
        Jedis resource = jedisPool.getResource();
        resource.publish(channel, message);
        resource.close();
    }
}
