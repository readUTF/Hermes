package com.github.readutf.hermes.serializer;

public interface StringSerializer<T> {

    String serialize(T var1) throws Exception;

    T deserialize(String var1) throws Exception;
}