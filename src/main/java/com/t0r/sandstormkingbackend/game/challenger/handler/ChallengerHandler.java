package com.t0r.sandstormkingbackend.game.challenger.handler;

import cn.hutool.core.io.IoUtil;
import cn.hutool.json.JSONUtil;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.*;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.LevelEnum;
import com.t0r.sandstormkingbackend.handler.GamePlayHandler;
import com.t0r.sandstormkingbackend.model.entity.Room;
import com.t0r.sandstormkingbackend.model.entity.RoomMember;
import com.t0r.sandstormkingbackend.service.RoomService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ChallengerHandler {

    @javax.annotation.Resource
    private RedisTemplate<String, Object> redisTemplate;

    @javax.annotation.Resource
    private GamePlayHandler gamePlayHandler;

    @javax.annotation.Resource
    private RoomService roomService;

    // 卡牌 ID -> Card
    private final static Map<Integer, Card> cardMap = new HashMap<>();

    // 房间 ID -> RoomDecks
    private final static Map<Long, RoomDecks> roomDecksMap = new ConcurrentHashMap<>();

    // 房间 ID -> 用户 ID -> ChallengerPlayer
    private final static Map<Long, Map<Long, ChallengerPlayer>> challengerPlayersMap = new ConcurrentHashMap<>();

    public void test() {
        loadCardMap();
//        for (Map.Entry<Integer, Card> entry : cardMap.entrySet()) {
//            System.out.println(entry.getKey() + ":" + entry.getValue());
//        }
        initGame(37L);
//        RoomDecks roomDecks = roomDecksMap.get(35L);

//        System.out.println("\n=== 房间1牌堆信息 ===");
//        if (roomDecks != null) {
//            for (Map.Entry<String, List<CardInstance>> entry : roomDecks.getMainDecks().entrySet()) {
//                System.out.println("牌堆 " + entry.getKey() + " 包含 " + entry.getValue().size() + " 张牌");
//                // 打印前几张牌作为示例
//                entry.getValue().stream().forEach(cardInstance ->
//                        System.out.println("  - 实例ID: " + cardInstance.getId() +
//                                ", 卡牌ID: " + cardInstance.getCardId() +
//                                ", 当前力量: " + cardInstance.getCurrentPower())
//                );
//                if (entry.getValue().size() > 5) {
//                    System.out.println("  ... 还有 " + (entry.getValue().size() - 5) + " 张牌");
//                }
//            }
//        }
        System.out.println("\n=== 房间1玩家信息 ===");
        Map<Long, ChallengerPlayer> challengerPlayers = challengerPlayersMap.get(37L);
        if (challengerPlayers != null) {
            for (Map.Entry<Long, ChallengerPlayer> entry : challengerPlayers.entrySet()) {
                for (CardInstance cardInstance : entry.getValue().getCardInstances()) {
                    System.out.println("玩家 " + entry.getKey() + " 持有卡牌 " + cardInstance.getId() +
                            "(" + cardInstance.getCardId() + ") " +
                            " 当前力量: " + cardInstance.getCurrentPower());
                }
            }
        }
    }

    public void initGame(Long roomId) {
        log.info("初始化游戏");

        ConcurrentHashMap<Long, ChallengerPlayer> challengerPlayers = new ConcurrentHashMap<>();
        Room room = roomService.getById(roomId);
        List<RoomMember> roomMembers = room.getRoomMembers();
        for (RoomMember member : roomMembers) {
            Long userId = member.getUserId();

            ChallengerPlayer challengerPlayer = new ChallengerPlayer();
            challengerPlayer.setUserId(userId);
            challengerPlayer.setCardInstances(new ArrayList<>());
            challengerPlayer.setCupCount(0);
            challengerPlayer.setExtraFanCount(0);
            challengerPlayer.setTotalFanCount(0);

            challengerPlayers.put(userId, challengerPlayer);
        }
        challengerPlayersMap.put(roomId, challengerPlayers);

        loadCardInstance(roomId);

    }

    public void loadCardMap() {
        log.info("加载卡牌数据");
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:cards/*.json");

            int totalCount = 0;
            for (Resource resource : resources) {
                String fileName = resource.getFilename();
                if (fileName == null || !fileName.endsWith(".json")) {
                    continue;
                }
                String jsonStr = IoUtil.readUtf8(resource.getInputStream());
                List<Card> cardList = JSONUtil.toList(JSONUtil.parseArray(jsonStr), Card.class);
                for (Card card : cardList) {
                    cardMap.put(card.getId(), card);
                }
                totalCount += cardList.size();
            }
            log.info("卡牌加载完成，文件数：{}，总数量：{}", resources.length, totalCount);
        } catch (Exception e) {
            log.error("加载卡牌数据失败", e);
        }
    }

    public void loadCardInstance(Long roomId) {
        log.info("加载房间 {} 的卡牌实例", roomId);

        RoomDecks roomDecks = new RoomDecks();
        int localId = 1;

        for (LevelEnum level : LevelEnum.values()) {
            if (level.isKept()) {
                roomDecks.addMainDeck(level.getValue(), new ArrayList<>()); // 主牌堆
                roomDecks.addDiscardDeck(level.getValue(), new ArrayList<>()); // 弃牌堆
            }
        }

        for (Card card : cardMap.values()) {
            String cardLevel = card.getLevel();
            LevelEnum levelEnum = LevelEnum.getEnumByValue(cardLevel);

            if (levelEnum != null && levelEnum.isKept()) {
                int count = card.getCount() != null ? card.getCount() : 1;
                for (int i = 0; i < count; i++) {
                    CardInstance instance = new CardInstance();
                    instance.setId(localId++);
                    instance.setCardId(card.getId());
                    instance.setCurrentPower(card.getBasePower());
                    roomDecks.getMainDeck(cardLevel).add(instance);
                }
            } else if (levelEnum != null) {
                Map<Long, ChallengerPlayer> longChallengerPlayerMap = challengerPlayersMap.get(roomId);
                for (ChallengerPlayer challengerPlayer : longChallengerPlayerMap.values()) {
                    for (int i = 0; i < card.getCount(); i++) {
                        CardInstance instance = new CardInstance();
                        instance.setId(localId++);
                        instance.setCardId(card.getId());
                        instance.setCurrentPower(card.getBasePower());
                        challengerPlayer.getCardInstances().add(instance);
                    }
                }
            }
        }

        roomDecksMap.put(roomId, roomDecks);

        for (String key : roomDecks.getMainDecks().keySet()) {
            log.info("房间 {} 牌堆 {} 有 {} 张牌", roomId, key, roomDecks.getMainDeck(key).size());
        }
    }


}
