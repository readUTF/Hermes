package com.github.readutf.hermes.pipline.listeners;

import com.github.readutf.hermes.Hermes;
import com.github.readutf.hermes.utils.LogUtil;
import com.github.readutf.hermes.wrapper.ParcelWrapper;
import lombok.Getter;

import java.lang.reflect.Method;
import java.util.*;

public class ParcelListenerManager {

    private final Hermes hermes;
    private @Getter final List<Class<?>> registeredClasses;
    private final Map<String, Map<Method, Object>> channelToObjectMethods;

    public ParcelListenerManager(Hermes hermes) {
        channelToObjectMethods = new HashMap<>();
        registeredClasses = new ArrayList<>();
        this.hermes = hermes;
    }

    public void call(String channel, UUID parcelId, String message) {

        Map<Method, Object> handlers = channelToObjectMethods.getOrDefault(channel, new HashMap<>());
        for (Map.Entry<Method, Object> entry : handlers.entrySet()) {
            Method method = entry.getKey();
            Object object = entry.getValue();

            Object result = null;

            try {
                if (method.getParameterCount() == 0) {
                    result = method.invoke(object);
                } else {
                    if (method.getParameterTypes()[0] == String.class) {
                        result = method.invoke(object, message);
                    } else {
                        result = method.invoke(object, new ParcelWrapper(channel, message));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (result == null) continue;
            hermes.sendResponse(parcelId, result);
        }
    }

    public void registerListeners(Object... objects) {
        for (Object object : objects) registerObject(object);
    }

    private void registerObject(Object object) {

        registeredClasses.add(object.getClass());
        LogUtil.log(String.format("Scanning %s for listeners", object.getClass().getSimpleName()));

        List<String> registered = new ArrayList<>();
        Method[] methods = object.getClass().getMethods();
        for (Method method : methods) {
            if (!method.isAnnotationPresent(ParcelListener.class)) continue;
            if (!isValidParameters(method.getParameterTypes())) continue;

            String channel = method.getAnnotation(ParcelListener.class).value();
            Map<Method, Object> handlers = channelToObjectMethods.getOrDefault(hermes.getPrefix() + "_" + channel, new HashMap<>());
            handlers.put(method, object);
            channelToObjectMethods.put(hermes.getPrefix() + "_" + channel, handlers);
            registered.add(method.getName());
        }
        LogUtil.log(String.format("Found %s listeners [%s]", registered.size(), String.join(", ", registered)));

    }

    public boolean isValidParameters(Class<?>[] args) {
        return args.length == 0 || (args.length == 1 && (args[0] == String.class) || args[0] == ParcelWrapper.class);
    }

}
