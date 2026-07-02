package com.fanconn.rideOS;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class RealtimeDeltaManager {

    private static RealtimeDeltaManager instance;

    private final ConcurrentHashMap<String, Object> lastValues =
            new ConcurrentHashMap<>();

    public static synchronized
    RealtimeDeltaManager getInstance() {
        if (instance == null) {
            instance = new RealtimeDeltaManager();
        }
        return instance;
    }

    public String changed(
            String key,
            Object value) {

        Object old =
                lastValues.get(key);

        if (Objects.equals(old, value)) {
            return null;
        }

        lastValues.put(key, value);

        return String.valueOf(value);
    }

    public Float changedFloat(String key, Float value) {

        Object old = lastValues.get(key);

        if (Objects.equals(old, value)) {
            return null;
        }

        if (value == null) {
            lastValues.remove(key);
            return null;
        }

        lastValues.put(key, value);
        return value;
    }

    public Integer changedInt(String key, Integer value) {

        Object old = lastValues.get(key);

        if (Objects.equals(old, value)) {
            return null;
        }

        if (value == null) {
            lastValues.remove(key);
            return null;
        }

        lastValues.put(key, value);
        return value;
    }

    public Double changedDouble(String key, Double value) {

        Object old = lastValues.get(key);

        if (Objects.equals(old, value)) {
            return null;
        }

        if (value == null) {
            lastValues.remove(key);
            return null;
        }

        lastValues.put(key, value);
        return value;
    }

    public String changed(String key, String value) {

        Object old = lastValues.get(key);

        if (Objects.equals(old, value)) {
            return null;
        }

        if (value == null) {
            lastValues.remove(key);
            return null;
        }

        lastValues.put(key, value);
        return value;
    }
}
