package com.github.readutf.hermes.utils;

import com.github.readutf.hermes.Hermes;
import com.github.readutf.hermes.pipline.listeners.ParcelListener;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.lang.reflect.Method;

public class ParcelListenerFinder {

    public static void findUnregistered(Class<?> baseClass, Hermes hermes) {
        LogUtil.log("WARNING: SCANNING ALL CLASSES IS SLOW");
        LogUtil.log("WARNING: SCANNING ALL CLASSES IS SLOW");
        LogUtil.log("WARNING: SCANNING ALL CLASSES IS SLOW");

        for (Class<?> aClass : ClassUtils.getClassesInPackage(baseClass)) {
            try {
                for (Method method : aClass.getMethods()) {
                    if (method.isAnnotationPresent(ParcelListener.class) && !hermes.getParcelListenerManager().getRegisteredClasses().contains(aClass)) {
                        LogUtil.log("Found unregistered parcel listener " + aClass.getName() + " for channel " + method.getAnnotation(ParcelListener.class).value());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
