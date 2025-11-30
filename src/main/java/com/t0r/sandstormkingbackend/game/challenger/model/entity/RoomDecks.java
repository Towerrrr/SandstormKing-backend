package com.t0r.sandstormkingbackend.game.challenger.model.entity;

import cn.hutool.core.io.IoUtil;
import cn.hutool.json.JSONUtil;
import com.t0r.sandstormkingbackend.exception.ErrorCode;
import com.t0r.sandstormkingbackend.exception.ThrowUtils;
import com.t0r.sandstormkingbackend.game.challenger.model.dto.RoomInitRequest;
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

import static com.t0r.sandstormkingbackend.game.challenger.constant.ChallengerConstant.MAX_PLAYER_COUNT;
import static com.t0r.sandstormkingbackend.game.challenger.handler.ChallengerHandler.*;

@Data
@Slf4j
public class RoomDecks {

//    region 不变域

    private Long roomId;

    private String version;

    private Integer totalPlayerCount;

    private Integer battlefieldCount;

    private boolean hasBot;

    // 回合数 -> 抽卡计划
    private Map<String, DrawSchedule> drawSchedules = new HashMap<>();

    // TODO 直接解析玩家中的战场属性来分配战场

//    endregion

//    region 变化域

    private String currentRound;

    // 玩家 ID -> ChallengerPlayer
    private Map<Long, ChallengerPlayer> challengerPlayers = new ConcurrentHashMap<>();

    // 回合数 -> 奖杯实例
    private Map<String, CupInstanceDeck> cupInstances = new ConcurrentHashMap<>();

    // 牌堆等级 -> 卡牌实例 （主牌堆、弃牌堆）
    private Map<String, List<CardInstance>> mainDecks = new ConcurrentHashMap<>();
    private Map<String, List<CardInstance>> discardDecks = new ConcurrentHashMap<>();

    // 用于临时战斗
    // 战场名 -> 玩家 ID -> 半场
    private Map<String, Map<Long, HalfBattlefield>> tempBattlefields = new ConcurrentHashMap<>();

//    endregion

    RoomDecks(RoomInitRequest roomInitRequest) {
        log.info("初始化房间: {}, 游戏：挑战者", roomId);

        // 不变域
        this.roomId = roomInitRequest.getRoomId();

        Integer playerCount = roomInitRequest.getPlayerCount();
        ThrowUtils.throwIf(playerCount < MAX_PLAYER_COUNT,
                ErrorCode.PARAMS_ERROR, "最多只能加入 " + MAX_PLAYER_COUNT + " 人");
        if(playerCount % 2 == 0) {
            this.totalPlayerCount = playerCount;
            this.hasBot = false;
        } else {
            this.totalPlayerCount = playerCount + 1;
            this.hasBot = true;
        }
        this.battlefieldCount = totalPlayerCount / 2;

        this.version = roomInitRequest.getVersion();
        loadDrawSchedule();

        // 变化域
        this.currentRound = RoundEnum.getFirstRound().getValue();
        initChallengerPlayers(roomInitRequest.getUserIds());
        initCupInstance();
        initCardInstance();
        resetBattlefield();
    }

    // TODO 后续先全部加载到Handler再从那边取
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

//    region 初始化变化域

    public void initChallengerPlayers(List<Long> userIds) {
        log.info("初始化房间 {} 的玩家信息", roomId);

        // 打乱战场分配
        List<Map<String, String>> battlefieldArrange = battlefieldArrangeMap.get(totalPlayerCount);
        List<Map<String, String>> battlefieldArrangeCopy = new ArrayList<>(battlefieldArrange);
        Collections.shuffle(battlefieldArrangeCopy);
        Deque<Map<String, String>> battlefieldArrangeQueue = new ArrayDeque<>(battlefieldArrangeCopy);

        for (Long userId : userIds) {
            ChallengerPlayer challengerPlayer = new ChallengerPlayer();
            challengerPlayer.setUserId(userId);
            challengerPlayer.setBattlefieldSchedules(battlefieldArrangeQueue.poll());
            // 玩家初始手牌在 initCardInstance() 中初始化

            challengerPlayers.put(userId, challengerPlayer);
        }
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
                for (ChallengerPlayer challengerPlayer : challengerPlayers.values()) {
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

//    endregion

    public void resetBattlefield() {
        log.info("重置房间 {} 的战场", roomId);

        // 创建战场
        BattlefieldEnum[] battlefieldEnums = BattlefieldEnum.values();
        for (int i = 0; i < battlefieldCount; i++) {
            BattlefieldEnum battlefieldEnum = battlefieldEnums[i];
            tempBattlefields.put(battlefieldEnum.getValue(), new ConcurrentHashMap<>());
        }

        // 玩家置入战场
        for (ChallengerPlayer challengerPlayer : challengerPlayers.values()) {
            String playerBattlefield = challengerPlayer.getBattlefieldSchedules().get(currentRound);
            tempBattlefields.get(playerBattlefield).put(challengerPlayer.getUserId(), new HalfBattlefield());
        }

    }

    public void nextRound() {
        log.info("房间 {} 进入下一轮", roomId);

        RoundEnum currentRound = RoundEnum.getByValue(this.currentRound);
        ThrowUtils.throwIf(currentRound == RoundEnum.getLastRound(),
                ErrorCode.PARAMS_ERROR, "已经是最后一轮了");
        currentRound = Objects.requireNonNull(currentRound).getNextRound();

        resetBattlefield();
    }


}

