package com.t0r.sandstormkingbackend.controller;

import com.t0r.sandstormkingbackend.common.BaseResponse;
import com.t0r.sandstormkingbackend.common.ResultUtils;
import com.t0r.sandstormkingbackend.game.challenger.handler.ChallengerHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/")
public class MainController {

    @Resource
    private ChallengerHandler challengerHandler;

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public BaseResponse<String> health() {
        return ResultUtils.success("ok");
    }

    @GetMapping("/test")
    public BaseResponse<String> test() {
        challengerHandler.test();
        return ResultUtils.success("ok");
    }

}

