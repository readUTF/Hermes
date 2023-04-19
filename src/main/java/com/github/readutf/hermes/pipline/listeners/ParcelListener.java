package com.github.readutf.hermes.pipline.listeners;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ParcelListener {

    String value();

}
