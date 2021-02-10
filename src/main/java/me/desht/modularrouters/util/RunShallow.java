package me.desht.modularrouters.util;

import java.util.HashSet;
import java.util.function.Function;

public class RunShallow<Key> {
    private final ThreadLocal<HashSet<Key>> running = ThreadLocal.withInitial(HashSet::new);

    public <Result> Result run(Key key, Function<Key, Result> defaultValue, Function<Key, Result> getter) {
        HashSet<Key> reading = running.get();

        if (reading.contains(key)) {
            return defaultValue.apply(key);
        } else {
            reading.add(key);
            try {
                return getter.apply(key);
            } catch (Exception err) {
                throw err;
            } finally {
                reading.remove(key);
            }
        }
    }
}
