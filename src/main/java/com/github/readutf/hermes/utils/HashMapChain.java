package com.github.readutf.hermes.utils;

import java.util.HashMap;

public class HashMapChain<A> extends HashMap<String, A> {

    public HashMapChain<A> add(String key, A value) {
        put(key, value);
        return this;
    }

}
