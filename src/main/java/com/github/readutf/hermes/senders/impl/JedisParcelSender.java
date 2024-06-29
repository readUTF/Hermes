package com.github.readutf.hermes.senders.impl;

import com.github.readutf.hermes.senders.ParcelSender;
import com.github.readutf.hermes.utils.LogUtil;
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
//        LogUtil.log("Sending parcel to " + channel + " with message " + message);
        resource.publish(channel, message);

        jedisPool.returnResource(resource);
    }
}
