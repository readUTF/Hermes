package com.github.readutf.utils;

import java.util.HashMap;

public class HashMapChain<A, B> {

    HashMap<A, B> map;

    public HashMapChain() {
        this.map = new HashMap<>();
    }

    public HashMapChain<A, B> put(A key, B value) {
        map.put(key, value);
        return this;
    }

    public HashMap<A, B> build() {
        return map;
    }

}
