package com.fanconn.rideOS.network;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TelemetryDeltaManager {

    private static final TelemetryDeltaManager instance =
            new TelemetryDeltaManager();

    public static TelemetryDeltaManager getInstance() {
        return instance;
    }

    private final Map<String, Object> cache =
            new HashMap<>();

    public String changed(String key, String value) {

        Object old = cache.get(key);

        if (Objects.equals(old, value)) {
            return null;
        }

        cache.put(key, value);
        return value;
    }

    public Integer changedInt(String key, int value) {

        Object old = cache.get(key);

        if (old instanceof Integer &&
                ((Integer) old) == value) {
            return null;
        }

        cache.put(key, value);
        return value;
    }

    public Float changedFloat(String key, float value) {

        Object old = cache.get(key);

        if (old instanceof Float &&
                Float.compare((Float) old, value) == 0) {
            return null;
        }

        cache.put(key, value);
        return value;
    }
    public Float changedFloatObj(String key, float value) {

        Object old = cache.get(key);

        if (old instanceof Float &&
                Float.compare((Float) old, value) == 0) {
            return null;
        }

        cache.put(key, value);

        return value;
    }

}