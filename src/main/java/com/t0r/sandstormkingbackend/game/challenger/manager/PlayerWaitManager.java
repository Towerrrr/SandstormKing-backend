package com.t0r.sandstormkingbackend.game.challenger.manager;

import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.cardSelector.CardSelectorRequest;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.cardSelector.CardSelectorResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class PlayerWaitManager {
    private final ConcurrentHashMap<String, MonoSink<CardSelectorResponse>> waitMap = new ConcurrentHashMap<>();

    public Mono<CardSelectorResponse> createWaitMono(String key) {
        return Mono.create(sink -> waitMap.put(key, sink));
    }

    public void completeWaitMono(String key, CardSelectorResponse value) {
        MonoSink<CardSelectorResponse> sink = waitMap.remove(key);
        if (sink != null) {
            sink.success(value);
        }
    }
}
