package com.github.readutf.hermes.pipline.listeners;

import com.github.readutf.hermes.wrapper.ParcelWrapper;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ParcelListenerManager {

    Map<String, Map<Method, Object>> channelToObjectMethods;

    public ParcelListenerManager() {
        channelToObjectMethods = new HashMap<>();
    }

    public void call(String channel, String message) {
        Map<Method, Object> handlers = channelToObjectMethods.getOrDefault(channel, new HashMap<>());
        for (Map.Entry<Method, Object> entry : handlers.entrySet()) {
            Method method = entry.getKey();
            Object object = entry.getValue();
            try {
                if(method.getParameterCount() == 0) {
                    method.invoke(object);
                } else {
                    if(method.getParameterTypes()[0] == String.class) {
                        method.invoke(object, message);
                    } else {
                        method.invoke(object, new ParcelWrapper(channel, message));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void registerListeners(Object... objects) {
        for (Object object : objects) registerObject(object);
    }

    private void registerObject(Object object) {
        System.out.println("registering " + object.getClass().getName());

        Method[] methods = object.getClass().getMethods();
        for (Method method : methods) {
            if(!method.isAnnotationPresent(ParcelListener.class)) continue;
            if(!isValidParameters(method.getParameterTypes())) continue;

            String channel = method.getAnnotation(ParcelListener.class).value();
            Map<Method, Object> handlers = channelToObjectMethods.getOrDefault(channel, new HashMap<>());
            handlers.put(method, object);
            channelToObjectMethods.put(channel, handlers);
        }
    }

    public boolean isValidParameters(Class<?>[] args) {
        return args.length == 0 || (args.length == 1 && (args[0] == String.class) || args[0] == ParcelWrapper.class);
    }

}
