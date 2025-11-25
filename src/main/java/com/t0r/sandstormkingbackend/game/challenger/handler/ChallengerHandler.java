package com.t0r.sandstormkingbackend.game.challenger.handler;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.json.JSONUtil;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.Card;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.ChallengerPlayer;
import com.t0r.sandstormkingbackend.handler.GamePlayHandler;
import com.t0r.sandstormkingbackend.model.dto.game.WebSocketRequestMessage;
import com.t0r.sandstormkingbackend.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ChallengerHandler {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private GamePlayHandler gamePlayHandler;

    private static Map<Integer, Card> cardMap;

    public static void main(String[] args) {
        ChallengerHandler challengerHandler = new ChallengerHandler();
        challengerHandler.loadCardMap();
        for (Map.Entry<Integer, Card> entry : cardMap.entrySet()) {
            System.out.println(entry.getKey() + ":" + entry.getValue());
        }
    }

    public void initGame(WebSocketRequestMessage webSocketRequestMessage,
                         WebSocketSession session, User user, Long roomId) {
        log.info("初始化游戏");

        Long userId = user.getId();

        ChallengerPlayer challengerPlayer = new ChallengerPlayer();
        challengerPlayer.setUserId(userId);
        // todo 后续重构
        challengerPlayer.setCardIds(Arrays.asList(1, 1, 1, 2, 3, 4));
        challengerPlayer.setCupCount(0);
        challengerPlayer.setExtraFanCount(0);
        challengerPlayer.setTotalFanCount(0);

        String roomKey = "room:" + roomId;
        String gameKey = roomKey + ":challenger";
        String playersKey = gameKey + ":players";
        String userIdStr = String.valueOf(userId);
        redisTemplate.opsForHash().put(playersKey, userIdStr, JSONUtil.toJsonStr(challengerPlayer));
    }

    public void loadCardMap() {
        log.info("加载卡牌数据");

        try {
            // 读取 resources/cards/0city_S.json 文件内容
            String jsonStr = ResourceUtil.readUtf8Str("cards/0city_S.json");

            // 解析成 List<Card>
            List<Card> cardList = JSONUtil.toList(JSONUtil.parseArray(jsonStr), Card.class);

            // 转成 Map<Integer, Card>
            Map<Integer, Card> map = new HashMap<>();
            for (Card card : cardList) {
                map.put(card.getId(), card);
            }
            cardMap = map;

            log.info("卡牌加载完成，数量：{}", cardMap.size());
        } catch (Exception e) {
            log.error("加载卡牌数据失败", e);
            cardMap = new HashMap<>();
        }
    }
}
