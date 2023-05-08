package com.github.readutf.hermes.quick;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.function.Consumer;
import java.util.function.Function;

public class JedisQuick {

    JedisPool jedisPool;

    public JedisQuick(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public void useRedis(Consumer<Jedis> jedisConsumer) {
        Jedis resource = jedisPool.getResource();
        jedisConsumer.accept(resource);
        jedisPool.returnResource(resource);
    }

    public <T> T getRedis(Function<Jedis, T> function) {

        Jedis resource = jedisPool.getResource();
        T apply = function.apply(resource);
        jedisPool.returnResource(resource);

        return apply;
    }

}
