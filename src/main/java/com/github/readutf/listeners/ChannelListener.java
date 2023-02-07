package com.github.readutf.listeners;

import com.google.gson.TypeAdapter;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)

public @interface ChannelListener {

    String value();

}
