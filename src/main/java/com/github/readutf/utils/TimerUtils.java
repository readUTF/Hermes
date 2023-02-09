package com.github.readutf.utils;

import lombok.Getter;

import java.util.Timer;
import java.util.TimerTask;

public class TimerUtils {

    @Getter(lazy = true) private static final Timer timer = new Timer();

    public static void runLater(Runnable runnable, int delay) {
        getTimer().schedule(new TimerTask() {
            @Override
            public void run() {
                runnable.run();
            }
        }, delay);
    }

}
