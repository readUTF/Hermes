package com.github.readutf.hermes.serialization;

public interface StringSerializer<T> {

    String serialize(T object);

    T deserialize(String bytes);

}
