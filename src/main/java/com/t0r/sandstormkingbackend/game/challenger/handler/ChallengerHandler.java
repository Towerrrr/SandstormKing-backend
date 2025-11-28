package com.t0r.sandstormkingbackend.game.challenger.handler;

import cn.hutool.core.io.IoUtil;
import cn.hutool.json.JSONUtil;
import com.t0r.sandstormkingbackend.exception.ErrorCode;
import com.t0r.sandstormkingbackend.exception.ThrowUtils;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.*;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.BattlefieldEnum;
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

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.t0r.sandstormkingbackend.game.challenger.constant.ChallengerConstant.MAX_PLAYER_COUNT;

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
        loadCardInstance(1L);
        loadCupInstance(1L);
        Map<String, CupInstanceDeck> cupInstances = roomDecksMap.get(1L).getCupInstances();
        for (Map.Entry<String, CupInstanceDeck> entry : cupInstances.entrySet()) {
            log.info(String.format("%s %s", entry.getKey(), entry.getValue()));
        }

    }

    public void initGame(Long roomId, String version, Integer playerCount) {
        log.info("初始化游戏");

        ThrowUtils.throwIf(playerCount < MAX_PLAYER_COUNT,
                ErrorCode.PARAMS_ERROR, "最多只能加入 " + MAX_PLAYER_COUNT + " 人");

        ConcurrentHashMap<Long, ChallengerPlayer> challengerPlayers = new ConcurrentHashMap<>();
        Room room = roomService.getById(roomId);
        List<RoomMember> roomMembers = room.getRoomMembers();
        for (RoomMember member : roomMembers) {
            Long userId = member.getUserId();

            ChallengerPlayer challengerPlayer = new ChallengerPlayer();
            challengerPlayer.setUserId(userId);
            challengerPlayer.setCardInstances(new ArrayList<>());
            challengerPlayer.setCupInstances(new ArrayList<>());
            challengerPlayer.setExtraFanCount(0);
            challengerPlayer.setTotalFanCount(0);

            challengerPlayers.put(userId, challengerPlayer);
        }
        challengerPlayersMap.put(roomId, challengerPlayers);

        loadCardInstance(roomId);
        loadDrawSchedule(roomId, version);
        loadCupInstance(roomId);
        loadBattlefield(roomId, playerCount);

    }

    public void loadBattlefield(Long roomId, Integer playerCount) {
        log.info("加载房间 {} 的战场", roomId);

        int battlefieldCount = playerCount % 2 + 1;

        Map<String, Map<Long, HalfBattlefield>> battlefields = roomDecksMap.get(roomId).getBattlefields();

        BattlefieldEnum[] battlefieldEnums = BattlefieldEnum.values();
        for (int i = 0; i < battlefieldCount; i++) {
            BattlefieldEnum battlefieldEnum = battlefieldEnums[i];
            battlefields.put(battlefieldEnum.getValue(), new ConcurrentHashMap<>());
        }

        log.info("房间 {} 的战场加载完成，共加载 {} 个战场", roomId, battlefieldCount);
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

    public void loadDrawSchedule(Long roomId, String version) {
        log.info("加载房间 {} 的抽卡计划，版本：{}", roomId, version);

        try {
            // todo 后续改成用 枚举类判断
            String fileName = "draw-schedules/" + version + ".json";
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource resource = resolver.getResource("classpath:" + fileName);
            if (!resource.exists()) {
                log.error("抽卡计划文件 {} 不存在", fileName);
                return;
            }
            String jsonStr = IoUtil.readUtf8(resource.getInputStream());

            // 解析为 List<DrawSchedule>
            List<DrawSchedule> drawScheduleList = JSONUtil.toList(JSONUtil.parseArray(jsonStr), DrawSchedule.class);

            Map<String, DrawSchedule> drawSchedules = roomDecksMap.get(roomId).getDrawSchedules();
            for (DrawSchedule drawSchedule : drawScheduleList) {
                drawSchedules.put(drawSchedule.getRound(), drawSchedule);
            }

            log.info("房间 {} 的抽卡计划（{}）加载成功，共 {} 轮", roomId, version, drawScheduleList.size());
        } catch (Exception e) {
            log.error("加载房间 {} 的抽卡计划失败", roomId, e);
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

        // 打乱主牌堆
        LevelEnum[] levelEnums = LevelEnum.values();
        for (LevelEnum levelEnum : levelEnums) {
            List<CardInstance> mainDeck = roomDecksMap.get(roomId).getMainDeck(levelEnum.getValue());
            Collections.shuffle(mainDeck);
        }

        roomDecksMap.put(roomId, roomDecks);

        for (String key : roomDecks.getMainDecks().keySet()) {
            log.info("房间 {} 牌堆 {} 有 {} 张牌", roomId, key, roomDecks.getMainDeck(key).size());
        }
    }

    public void loadCupInstance(Long roomId) {
        log.info("加载房间 {} 的奖杯实例", roomId);

        try {
            String fileName = "cup-schedule/cupInstances.json";
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource resource = resolver.getResource("classpath:" + fileName);
            if (!resource.exists()) {
                log.error("奖杯实例文件 {} 不存在", fileName);
                return;
            }
            String jsonStr = IoUtil.readUtf8(resource.getInputStream());

            Map<String, CupInstanceDeck> cupInstances = roomDecksMap.get(roomId).getCupInstances();

            List<CupInstanceDeck> cupInstanceDeckList = JSONUtil.toList(JSONUtil.parseArray(jsonStr), CupInstanceDeck.class);
            for (CupInstanceDeck cupInstanceDeck : cupInstanceDeckList) {
                cupInstanceDeck.parseCupInstance();

                // 打乱奖杯
                List<CupInstance> cupInstanceList = cupInstanceDeck.getCupInstanceList();
                Collections.shuffle(cupInstanceList);

                cupInstances.put(cupInstanceDeck.getRound(), cupInstanceDeck);
            }

            log.info("房间 {} 的奖杯实例加载成功，共 {} 轮", roomId, cupInstanceDeckList.size());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }


}
