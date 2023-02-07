package com.github.readutf.test;

import com.github.readutf.Hermes;
import com.github.readutf.listeners.ChannelListener;
import com.github.readutf.wrapper.ParcelResponse;
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
