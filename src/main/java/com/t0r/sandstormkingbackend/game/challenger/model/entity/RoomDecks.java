package com.t0r.sandstormkingbackend.game.challenger.model.entity;

import cn.hutool.core.io.IoUtil;
import cn.hutool.json.JSONUtil;
import com.t0r.sandstormkingbackend.exception.ErrorCode;
import com.t0r.sandstormkingbackend.exception.ThrowUtils;
import com.t0r.sandstormkingbackend.game.challenger.model.dto.RoomConfig;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.BattlefieldEnum;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.LevelEnum;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.RoundEnum;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.t0r.sandstormkingbackend.game.challenger.handler.ChallengerHandler.*;

@Data
@Slf4j
public class RoomDecks {

//    region 不变域

    private Long roomId;

    private String version;

    private Integer totalPlayerCount;

    private boolean hasBot;

    // 回合数 -> 抽卡计划
    private Map<String, DrawSchedule> drawSchedules = new HashMap<>();

    // TODO 直接解析玩家中的战场属性来分配战场

//    endregion

//    region 变化域

    private String currentRound;

    // 回合数 -> 奖杯实例
    private Map<String, CupInstanceDeck> cupInstances = new ConcurrentHashMap<>();

    // 牌堆等级 -> 卡牌实例 （主牌堆、弃牌堆）
    private Map<String, List<CardInstance>> mainDecks = new ConcurrentHashMap<>();
    private Map<String, List<CardInstance>> discardDecks = new ConcurrentHashMap<>();

    // 用于临时战斗
    // 战场名 -> 玩家 ID -> 半场
    private Map<String, Map<Long, HalfBattlefield>> tempBattlefields = new ConcurrentHashMap<>();

//    endregion

    RoomDecks(RoomConfig roomConfig) {
        this.roomId = roomConfig.getRoomId();

        Integer playerCount = roomConfig.getPlayerCount();
        if(playerCount % 2 == 0) {
            this.totalPlayerCount = playerCount;
            this.hasBot = false;
        } else {
            this.totalPlayerCount = playerCount + 1;
            this.hasBot = true;
        }

        this.version = roomConfig.getVersion();
        loadDrawSchedule();

    }

    public void loadDrawSchedule() {
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

            Map<String, DrawSchedule> drawSchedules = getDrawSchedules();
            for (DrawSchedule drawSchedule : drawScheduleList) {
                drawSchedules.put(drawSchedule.getRound(), drawSchedule);
            }

            log.info("房间 {} 的抽卡计划（{}）加载成功，共 {} 轮", roomId, version, drawScheduleList.size());
        } catch (Exception e) {
            log.error("加载房间 {} 的抽卡计划失败", roomId, e);
        }
    }

    public void loadBattlefieldSchedule() {
        log.info("加载房间 {} 的战场计划", roomId);

        List<Map<String, String>> maps = battlefieldArrangeMap.get(totalPlayerCount);
    }

    public void initCardInstance() {
        log.info("初始化房间 {} 的卡牌实例", roomId);

        int localId = 1;

        for (LevelEnum level : LevelEnum.values()) {
            if (level.isKept()) {
                mainDecks.put(level.getValue(), new ArrayList<>()); // 主牌堆
                discardDecks.put(level.getValue(), new ArrayList<>()); // 弃牌堆
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
                    mainDecks.get(cardLevel).add(instance);
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
            Collections.shuffle(mainDecks.get(levelEnum.getValue()));
        }

        log.info("房间 {} 的卡牌实例初始化成功，共 {} 张", roomId, localId - 1);
    }

    public void initCupInstance() {
        log.info("初始化房间 {} 的奖杯实例", roomId);

        try {
            String fileName = "cup-schedule/cupInstances.json";
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource resource = resolver.getResource("classpath:" + fileName);
            if (!resource.exists()) {
                log.error("奖杯实例文件 {} 不存在", fileName);
                return;
            }
            String jsonStr = IoUtil.readUtf8(resource.getInputStream());

            Map<String, CupInstanceDeck> cupInstances = getCupInstances();

            List<CupInstanceDeck> cupInstanceDeckList = JSONUtil.toList(JSONUtil.parseArray(jsonStr), CupInstanceDeck.class);
            for (CupInstanceDeck cupInstanceDeck : cupInstanceDeckList) {
                cupInstanceDeck.parseCupInstance();

                // 打乱奖杯
                List<CupInstance> cupInstanceList = cupInstanceDeck.getCupInstanceList();
                Collections.shuffle(cupInstanceList);

                cupInstances.put(cupInstanceDeck.getRound(), cupInstanceDeck);
            }

            log.info("房间 {} 的奖杯实例初始化成功，共 {} 轮", roomId, cupInstanceDeckList.size());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void initBattlefield() {
        log.info("初始化房间 {} 的战场", roomId);

        int battlefieldCount = totalPlayerCount / 2;

        BattlefieldEnum[] battlefieldEnums = BattlefieldEnum.values();
        for (int i = 0; i < battlefieldCount; i++) {
            BattlefieldEnum battlefieldEnum = battlefieldEnums[i];
            tempBattlefields.put(battlefieldEnum.getValue(), new ConcurrentHashMap<>());
        }

        log.info("房间 {} 的战场初始化完成，共初始化 {} 个战场", roomId, battlefieldCount);
    }

    public void nextRound() {
        log.info("房间 {} 进入下一轮", roomId);

        RoundEnum currentRound = RoundEnum.getByValue(getCurrentRound());
        ThrowUtils.throwIf(currentRound == RoundEnum.getLastRound(),
                ErrorCode.PARAMS_ERROR, "已经是最后一轮了");
        if (currentRound != null) {
            setCurrentRound(Objects.requireNonNull(currentRound.getNextRound()).getValue());
        }

    }


}

