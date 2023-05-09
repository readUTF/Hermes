package com.github.readutf.hermes.inbuilt;

import com.github.readutf.hermes.Hermes;
import com.github.readutf.hermes.pipline.listeners.ParcelListener;
import com.github.readutf.hermes.wrapper.ParcelWrapper;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class PingCalculator {

    Hermes hermes;

    public PingCalculator(Hermes hermes) {
        this.hermes = hermes;
        hermes.addParcelListener(this);
    }

    @ParcelListener("PING")
    public long handlePing() {
        return System.currentTimeMillis();
    }

    public long calculatePing(boolean fullDebug) {
        CompletableFuture<Long> future = new CompletableFuture<>();
        long start = System.currentTimeMillis();
        hermes.sendParcel("PING", System.currentTimeMillis(), parcelWrapper -> {
            long end = System.currentTimeMillis();
            Long remote = parcelWrapper.get(Long.class);
            System.out.println("Local -> Remote: " + (remote - start) + "ms");
            System.out.println("Remote -> Local: " + (end - remote) + "ms");
            System.out.println("Total: " + (end - start) + "ms");

            future.complete(end - start);
        });

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            return -1;
        }
    }

}
