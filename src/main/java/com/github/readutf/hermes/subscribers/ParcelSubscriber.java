package com.github.readutf.hermes.subscribers;

import com.github.readutf.hermes.Hermes;
import com.github.readutf.hermes.pipline.ParcelConsumer;

public interface ParcelSubscriber {

    void subscribe(Hermes hermes, ParcelConsumer consumer);

}
