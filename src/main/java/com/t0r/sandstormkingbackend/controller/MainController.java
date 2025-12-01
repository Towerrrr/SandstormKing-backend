package com.t0r.sandstormkingbackend.controller;

import com.t0r.sandstormkingbackend.common.BaseResponse;
import com.t0r.sandstormkingbackend.common.ResultUtils;
import com.t0r.sandstormkingbackend.game.challenger.handler.ChallengerGameManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/")
public class MainController {

    @Resource
    private ChallengerGameManager challengerGameManager;

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public BaseResponse<String> health() {
        return ResultUtils.success("ok");
    }

    @GetMapping("/test")
    public BaseResponse<String> test() {
        return ResultUtils.success("ok");
    }

}

