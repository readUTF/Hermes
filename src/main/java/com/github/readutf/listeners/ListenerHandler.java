package com.github.readutf.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.readutf.Hermes;
import com.github.readutf.test.ListenerTest;
import com.github.readutf.wrapper.ParcelResponse;
import com.github.readutf.wrapper.ParcelWrapper;
import com.readutf.uls.Logger;
import lombok.SneakyThrows;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ListenerHandler {

    private static Logger logger = Hermes.getLoggerFactory().getLogger(ListenerHandler.class);

    private Hermes hermes;
    private ObjectMapper objectMapper;

    HashMap<String, List<Method>> channelListeners;
    HashMap<Class<?>, Object> objectInstances;

    ClassLoader classLoader;

    public ListenerHandler(Hermes hermes, ClassLoader classLoader) {
        this.hermes = hermes;
        this.channelListeners = new HashMap<>();
        this.objectInstances = new HashMap<>();
        this.objectMapper = hermes.getObjectMapper();
        this.classLoader = classLoader;
    }

    @SneakyThrows
    public void handleParcel(String channel, ParcelWrapper wrapper) {
        List<Method> methods = channelListeners.get(channel);

        if(methods == null) return;


        for (Method method : methods) {
            Logger logger1 = Hermes.getLoggerFactory().getLogger(method.getDeclaringClass());

            Object instance = objectInstances.get(method.getDeclaringClass());
            if (instance == null) {
                logger1.debug("Could not call method, no instance found.");
                continue;
            }
            ParcelResponse response;
            ChannelListener channelListener = method.getAnnotation(ChannelListener.class);

            if (method.getParameterTypes().length == 0) {
                response = invokeMethod(method, instance);
            } else if (method.getParameterTypes().length == 1) {
                try {
                    logger1.debug("data: " + wrapper);
                    Class<?> classType = classLoader.loadClass(method.getParameterTypes()[0].getName());

                    if (hermes.getTypeAdapters().containsKey(channelListener.value())) {
                        response = invokeMethod(method, instance, objectMapper.convertValue(wrapper.getData(), hermes.getTypeAdapters().get(channelListener.value())));
                    } else {
                        response = invokeMethod(method, instance, objectMapper.convertValue(wrapper.getData(), classType));
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            } else {
                logger1.debug(method.getName() + " cannot handle parcel.");
                return;
            }

            if(response != null) hermes.sendResponse(channel, wrapper.getParcelId(), response);
        }

    }

    @SneakyThrows
    public ParcelResponse invokeMethod(Method method, Object instance, Object... props) {
        Object invoke = method.invoke(instance, props);
        if (invoke instanceof ParcelResponse) return (ParcelResponse) invoke;
        return null;
    }

    public void registerChannelListener(Object instance) {
        Class<?> clazz = instance.getClass();
        if (!objectInstances.containsKey(clazz)) objectInstances.put(clazz, instance);
        List<Method> methods = Stream.of(clazz.getMethods()).filter(method -> method.isAnnotationPresent(ChannelListener.class)).collect(Collectors.toList());
        logger.debug("Found " + methods.size() + " handlers in " + instance.getClass().getSimpleName());
        for (Method method : methods) {
            String channel = method.getAnnotation(ChannelListener.class).value();
            List<Method> methods1 = channelListeners.getOrDefault(channel, new ArrayList<>());
            methods1.add(method);
            channelListeners.put(channel, methods1);
        }
    }

    public void registerChannelListener(Class<?> clazz, Object... props) {
        if (!clazz.isAnnotationPresent(ChannelListener.class)) {
            throw new RuntimeException("Class " + clazz.getName() + " is not annotated with @ChannelListener");
        }
        Object instance = getInstance(clazz, props);
        if (instance == null) return;

        registerChannelListener(instance);
    }

    public Object getInstance(Class<?> clazz, Object... props) {
        if (objectInstances.containsKey(clazz)) {
            return objectInstances.get(clazz);
        }
        Constructor<?> constructor = findConstructor(clazz, props);
        if (constructor == null) return null;
        try {
            return constructor.newInstance(props);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }


    public Constructor<?> findConstructor(Class<?> clazz, Object... props) {
        Class<?>[] types = new Class<?>[props.length];
        for (int i = 0; i < props.length; i++) {
            types[i] = props[i].getClass();
        }
        Constructor<?> empty = null;
        for (Constructor<?> constructor : clazz.getConstructors()) {
            if (constructor.getParameterTypes().length == 0) empty = constructor;
            if (Arrays.equals(types, constructor.getParameterTypes())) return constructor;
        }
        return empty;
    }

}
