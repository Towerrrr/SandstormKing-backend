package com.t0r.sandstormkingbackend.game.challenger.handler;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ChallengerHandlerTest {

    @Resource
    private ChallengerHandler challengerHandler;

    @Test
    void test() {
        challengerHandler.test();
    }

}