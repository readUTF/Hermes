package com.github.readutf.hermes.senders.impl;

import com.github.readutf.hermes.senders.ParcelSender;
import lombok.SneakyThrows;

public class MySQLParcelSender implements ParcelSender {

    @SneakyThrows
    public MySQLParcelSender() {
        Class.forName("com.mysql.jdbc.Driver");


    }

    @Override
    public void send(String channel, String message) {

    }
}
