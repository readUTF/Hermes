package com.github.readutf.hermes.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.readutf.hermes.Hermes;
import com.github.readutf.hermes.wrapper.ParcelResponse;
import com.github.readutf.hermes.wrapper.ParcelWrapper;
import com.readutf.uls.Logger;
import lombok.SneakyThrows;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ListenerHandler {


    private Hermes hermes;
    private ObjectMapper objectMapper;
    private final Logger logger;

    HashMap<String, List<Method>> channelListeners;
    HashMap<Class<?>, Object> objectInstances;

    ClassLoader classLoader;

    public ListenerHandler(Hermes hermes, ClassLoader classLoader) {
        this.hermes = hermes;
        this.logger = hermes.getLogger();
        this.channelListeners = new HashMap<>();
        this.objectInstances = new HashMap<>();
        this.objectMapper = hermes.getObjectMapper();
        this.classLoader = classLoader;
    }

    @SneakyThrows
    public void handleParcel(String channel, UUID parcelId, Object data) {
        HashSet<Method> methods = new HashSet<>(channelListeners.getOrDefault(channel, new ArrayList<>()));
        for (String s : channelListeners.keySet()) {
            if (isMatch(channel, s)) {
                methods.addAll(channelListeners.get(s));
            }
        }

        if (methods.isEmpty()) return;


        for (Method method : new ArrayList<>(methods)) {
            Logger logger1 = hermes.getLoggerFactory().getLogger(method.getDeclaringClass());

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
                    logger1.debug("data: " + data);
                    Class<?> classType = classLoader.loadClass(method.getParameterTypes()[0].getName());

                    if (hermes.getTypeAdapters().containsKey(channelListener.value())) {
                        response = invokeMethod(method, instance, objectMapper.convertValue(data, hermes.getTypeAdapters().get(channelListener.value())));
                    } else {
                        response = invokeMethod(method, instance, objectMapper.convertValue(data, classType));
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            } else {
                logger1.debug(method.getName() + " cannot handle parcel.");
                return;
            }

            if (parcelId != null && response != null) hermes.sendResponse(channel, parcelId, response);
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

    public boolean isMatch(String s, String p) {
        if (s == null || p == null) {
            return false;
        }
        int m = s.length();
        int n = p.length();
        boolean[][] dp = new boolean[2][n + 1];
        char[] ss = s.toCharArray();
        char[] pp = p.toCharArray();
        int curr = 0;
        int prev = 1;

        // dp[0][j] = false; dp[i][0] = false;

        for (int i = 0; i <= m; i++) {
            prev = curr;
            curr = 1 - prev;
            for (int j = 0; j <= n; j++) {
                if (i == 0 && j == 0) {
                    dp[curr][j] = true;
                    continue;
                }
                if (j == 0) { // When p is empty but s is not empty, should not match
                    dp[curr][j] = false;
                    continue;
                }
                dp[curr][j] = false;
                if (pp[j - 1] != '*') {
                    if (i >= 1 && (ss[i - 1] == pp[j - 1] || pp[j - 1] == '?')) {
                        dp[curr][j] = dp[prev][j - 1];
                    }
                } else {
                    dp[curr][j] |= dp[curr][j - 1];// '*' -> empty
                    if (i >= 1) { // '*' matches one or more of any character
                        dp[curr][j] |= dp[prev][j];
                    }
                }
            }
        }
        return dp[curr][n];
    }

}
