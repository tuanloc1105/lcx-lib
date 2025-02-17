package vn.com.lcx.common.utils;

import lombok.Getter;

import java.util.concurrent.ConcurrentHashMap;

public class CacheWithDurationUtils<K, V> {
    private final ConcurrentHashMap<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
    private final long expiryDuration;

    public CacheWithDurationUtils(long expiryDurationInMillis) {
        this.expiryDuration = expiryDurationInMillis;
    }

    public void put(K key, V value) {
        cache.put(key, new CacheEntry<>(value, expiryDuration));
    }

    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry == null || entry.isExpired()) {
            cache.remove(key);
            return null;
        }
        return entry.getValue();
    }

    @Getter
    private static class CacheEntry<V> {
        private final V value;
        private final long expiryTime;

        public CacheEntry(V value, long expiryDuration) {
            this.value = value;
            this.expiryTime = System.currentTimeMillis() + expiryDuration;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
}
