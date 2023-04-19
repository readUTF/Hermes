package com.github.readutf.hermes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.readutf.hermes.pipline.listeners.ParcelListener;
import com.github.readutf.hermes.senders.impl.JedisParcelSender;
import com.github.readutf.hermes.subscribers.impl.JedisParcelSubscriber;
import com.github.readutf.hermes.wrapper.ParcelWrapper;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Test {

    public static void main(String[] args) throws Exception {
        Hermes build = new Hermes.Builder()
                .prefix("test")
                .addListener(new TestClass())
                .parcelSubscriber(new JedisParcelSubscriber(new JedisPool("localhost", 6379)))
                .parcelSender(new JedisParcelSender(new JedisPool("localhost", 6379)))
                .build();

        build.connect();

        build.sendParcel("test", new ArrayList<>(Arrays.asList(1, 2)));
    }

    public static class TestClass {

        @ParcelListener("test")
        public void test(ParcelWrapper parcelWrapper) {

            List<Integer> integers = parcelWrapper.get(new TypeReference<List<Integer>>() {});
            System.out.println(integers);
        }

    }

}
