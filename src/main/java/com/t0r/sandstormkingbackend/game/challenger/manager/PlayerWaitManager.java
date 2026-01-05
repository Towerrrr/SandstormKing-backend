package com.t0r.sandstormkingbackend.game.challenger.manager;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class PlayerWaitManager {
    private final ConcurrentHashMap<String, MonoSink<?>> waitMap = new ConcurrentHashMap<>();

    public <T> Mono<T> createWaitMono(String key, Class<T> clazz) {
        return Mono.create(sink -> {
            waitMap.put(key, sink);

            sink.onDispose(() -> waitMap.remove(key, sink));
        });
    }

    @SuppressWarnings("unchecked")
    public void completeWaitMono(String key, Object value) {
        MonoSink<?> rawSink = waitMap.remove(key);
        if (rawSink != null) {
            MonoSink<Object> sink = (MonoSink<Object>) rawSink;

            try {
                sink.success(value);
            } catch (Exception e) {
                sink.error(e);
            }
        }
    }
}
