package com.github.readutf.hermes.serialization;

public interface StringSerializer<T> {

    String serialize(T object) throws Exception;

    T deserialize(String bytes) throws Exception;

}
