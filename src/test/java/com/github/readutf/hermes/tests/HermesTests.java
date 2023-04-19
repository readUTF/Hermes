package com.github.readutf.hermes.tests;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.readutf.hermes.Hermes;
import com.github.readutf.hermes.pipline.listeners.ParcelListener;
import com.github.readutf.hermes.senders.impl.JedisParcelSender;
import com.github.readutf.hermes.subscribers.impl.JedisParcelSubscriber;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.JedisPool;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HermesTests {

    private static ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void test() {

    }

}
