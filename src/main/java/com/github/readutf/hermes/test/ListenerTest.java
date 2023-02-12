package com.github.readutf.hermes.test;

import com.github.readutf.hermes.Hermes;
import com.github.readutf.hermes.listeners.ChannelListener;
import com.github.readutf.hermes.wrapper.ParcelResponse;
import com.readutf.uls.Logger;

import java.util.HashMap;

public class ListenerTest {

    private static Logger logger = Hermes.getLoggerFactory().getLogger(ListenerTest.class);

    @ChannelListener("test")
    public ParcelResponse onReceive(HashMap<String, Object> data) {
        logger.debug("listener found data: " + data);
        return new ParcelResponse(ParcelResponse.ParcelResponseType.SUCCESS, new HashMap<>());
    };

}
