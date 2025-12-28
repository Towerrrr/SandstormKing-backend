package com.t0r.sandstormkingbackend.game.challenger.model.entity;

import cn.hutool.core.io.IoUtil;
import cn.hutool.json.JSONUtil;
import com.t0r.sandstormkingbackend.Util.MyListUtil;
import com.t0r.sandstormkingbackend.exception.ErrorCode;
import com.t0r.sandstormkingbackend.exception.ThrowUtils;
import com.t0r.sandstormkingbackend.game.challenger.model.dto.InitGameRequest;
import com.t0r.sandstormkingbackend.game.challenger.model.dto.StartBattleResponse;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield.Battle;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield.BattleStateEnum;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield.Battlefield;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.BattlefieldEnum;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.LevelEnum;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.RoundEnum;
import com.t0r.sandstormkingbackend.game.challenger.model.event.PlayerReadyEvent;
import com.t0r.sandstormkingbackend.game.challenger.model.event.StartBattleEvent;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.t0r.sandstormkingbackend.game.challenger.constant.ChallengerConstant.MAX_PLAYER_COUNT;
import static com.t0r.sandstormkingbackend.game.challenger.handler.ChallengerGameManager.*;

@Data
@Slf4j
public class RoomGameState {

//    region 不变域

    private Long roomId;

    private String version;

    private Integer totalPlayerCount;

    private Integer battlefieldCount;

    private boolean hasBot;

    // 回合数 -> 抽卡计划
    private Map<String, DrawSchedule> drawSchedules = new HashMap<>();

    private final ApplicationEventPublisher eventPublisher;

    // TODO 直接解析玩家中的战场属性来分配战场

//    endregion

//    region 变化域

    private String currentRound;

    // 玩家 ID -> ChallengerPlayer
    private Map<Long, ChallengerPlayer> challengerPlayers = new ConcurrentHashMap<>();

    // 回合数 -> 奖杯实例
    private Map<String, CupInstanceDeck> cupInstances = new ConcurrentHashMap<>();

    // 牌堆等级 -> 卡牌实例 （主牌堆、弃牌堆）
    private Map<String, LinkedList<CardInstance>> mainDecks = new ConcurrentHashMap<>();
    private Map<String, LinkedList<CardInstance>> discardDecks = new ConcurrentHashMap<>();

    // 用于临时战斗
    // 战场名 -> 战场
    private Map<String, Battlefield> tempBattlefields = new ConcurrentHashMap<>();

//    endregion

//    region 构造方法

    public RoomGameState(InitGameRequest initGameRequest, ApplicationEventPublisher eventPublisher) {
        log.info("初始化房间: {}, 游戏：挑战者", roomId);

        // 不变域
        this.roomId = initGameRequest.getRoomId();

        Integer playerCount = initGameRequest.getPlayerCount();
        ThrowUtils.throwIf(playerCount > MAX_PLAYER_COUNT,
                ErrorCode.PARAMS_ERROR, "最多只能加入 " + MAX_PLAYER_COUNT + " 人");
        if (playerCount % 2 == 0) {
            this.totalPlayerCount = playerCount;
            this.hasBot = false;
        } else {
            this.totalPlayerCount = playerCount + 1;
            this.hasBot = true;
        }
        this.battlefieldCount = totalPlayerCount / 2;

        this.version = initGameRequest.getVersion();
        loadDrawSchedule();
        this.eventPublisher = eventPublisher;

        // 变化域
        this.currentRound = RoundEnum.getFirstRound().getValue();
        initChallengerPlayers(initGameRequest.getUserIds());
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

            for (DrawSchedule drawSchedule : drawScheduleList) {
                drawSchedules.put(drawSchedule.getRound(), drawSchedule);
            }

            log.info("房间 {} 的抽卡计划（{}）加载成功，共 {} 轮", roomId, version, drawScheduleList.size());
        } catch (Exception e) {
            log.error("加载房间 {} 的抽卡计划失败", roomId, e);
        }
    }

//    endregion

//    region 初始化变化域

    public void initChallengerPlayers(Set<Long> userIds) {
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
                mainDecks.put(level.getValue(), new LinkedList<>()); // 主牌堆
                discardDecks.put(level.getValue(), new LinkedList<>()); // 弃牌堆
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
                    mainDecks.get(cardLevel).add(instance);
                }
            } else if (levelEnum != null) {
                for (ChallengerPlayer challengerPlayer : challengerPlayers.values()) {
                    for (int i = 0; i < card.getCount(); i++) {
                        CardInstance instance = new CardInstance();
                        instance.setId(localId++);
                        instance.setCardId(card.getId());
                        challengerPlayer.getHandCardInstances().add(instance);
                    }
                }
            }
        }

        // 打乱主牌堆
        LevelEnum[] levelEnums = LevelEnum.values();
        for (LevelEnum levelEnum : levelEnums) {
            if (levelEnum.isKept()) {
                Collections.shuffle(mainDecks.get(levelEnum.getValue()));
            }
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
            tempBattlefields.put(battlefieldEnum.getValue(),
                    new Battlefield(battlefieldEnum.getValue(), currentRound, challengerPlayers, eventPublisher));
        }

    }

//    region 构筑阶段

    /**
     * 1. 选择选项（第一次抽卡）
     * 2. 确认选择 / 再次抽卡 / 部分选择 & 再次抽卡
     * 3. 确认选择
     */
    public LinkedList<CardInstance> buildCardInstances(Long userId, Integer OptionId, Set<Integer> selectedCardInstanceIds) {
        log.info("用户 {} 构筑卡牌, 选项 {}", userId, OptionId);

        ChallengerPlayer currentPlayer = challengerPlayers.get(userId);
        Option option = getOption(OptionId);
        String level = option.getLevel();
        Integer drawCount = option.getDrawCount();
        Integer fanCount = option.getFanCount();

        if (selectedCardInstanceIds == null || selectedCardInstanceIds.isEmpty()) {
            if (currentPlayer.isSecondSelect()) {
                log.info("用户 {} 第二次抽取卡牌（第一次未选择）", userId);
                selectAndDiscardCardInstances(currentPlayer, null);
                drawCardInstances(currentPlayer, level);
                currentPlayer.setSecondSelect(false);
            } else {
                log.info("用户 {} 第一次抽取卡牌", userId);
                drawCardInstances(currentPlayer, level);
                currentPlayer.setExtraFanCount(currentPlayer.getExtraFanCount() + fanCount);
                currentPlayer.setSecondSelect(true);
            }
        } else {
            selectAndDiscardCardInstances(currentPlayer, selectedCardInstanceIds);

            Set<CardInstance> selectedCards = currentPlayer.getSelectedCards();
            ThrowUtils.throwIf(selectedCards.size() > drawCount, ErrorCode.PARAMS_ERROR,
                    "选择的卡牌数不可超过" + drawCount);
            if (selectedCards.size() == drawCount) {
                log.info("用户 {} 确认选择卡牌", userId);
                confirmSelect(userId);
            } else {
                log.info("用户 {} 部分选择，再次抽卡", userId);
                drawCardInstances(currentPlayer, level);
            }
            currentPlayer.setSecondSelect(false);
        }

        return currentPlayer.getTempSelectedCardInstances();
    }

    private void confirmSelect(Long userId) {
        ChallengerPlayer currentPlayer = challengerPlayers.get(userId);
        Set<CardInstance> selectedCards = currentPlayer.getSelectedCards();
        LinkedList<CardInstance> handCardInstances = currentPlayer.getHandCardInstances();
        handCardInstances.addAll(selectedCards);
        selectedCards.clear();
    }

    /**
     * 根据 OptionId 获取当前回合的选项
     */
    private Option getOption(Integer OptionId) {
        List<Option> options = drawSchedules.get(currentRound).getOptions();
        for (Option option : options) {
            if (option.getId().equals(OptionId)) {
                return option;
            }
        }
        return null;
    }

    private void drawCardInstances(ChallengerPlayer currentPlayer, String level) {
        final int DRAW_COUNT = 5;

        if (mainDecks.get(level).size() < DRAW_COUNT) {
            LinkedList<CardInstance> shuffledDiscardDeck = MyListUtil.shuffleLinkedList(discardDecks.get(level));
            mainDecks.get(level).addAll(shuffledDiscardDeck);
            discardDecks.get(level).clear();
        }

        for (int i = 0; i < DRAW_COUNT; i++) {
            CardInstance cardInstance = mainDecks.get(level).removeFirst();
            currentPlayer.getTempSelectedCardInstances().add(cardInstance);
        }
    }

    private void selectAndDiscardCardInstances(ChallengerPlayer currentPlayer, Set<Integer> selectedCardInstanceIds) {
        LinkedList<CardInstance> tempSelectedCardInstances = currentPlayer.getTempSelectedCardInstances();
        Set<CardInstance> selectedCards = currentPlayer.getSelectedCards();
        for (CardInstance cardInstance : tempSelectedCardInstances) {
            String level = cardMap.get(cardInstance.getCardId()).getLevel();
            if (selectedCardInstanceIds != null && selectedCardInstanceIds.contains(cardInstance.getId())) {
                selectedCards.add(cardInstance);
            } else {
                discardDecks.get(level).add(cardInstance);
            }
        }
        tempSelectedCardInstances.clear();
    }

    public void readyBattle(String battlefield, Long userId) {
        log.info("用户 {} 确认准备，战场 {}", userId, battlefield);
        Battlefield battlefield1 = tempBattlefields.get(battlefield);

        Long opponentId = battlefield1.readyBattle(userId);
        if (battlefield1.checkAllReady()) {
            battlefield1.startBattle(challengerPlayers);
            Long startPlayerId = battlefield1.getStartPlayerId();
            String startWay = battlefield1.getStartWay();
            LinkedList<Battle> battleList = battlefield1.getBattleList();
            StartBattleResponse startBattleResponse = new StartBattleResponse(startPlayerId, startWay, battleList);

            eventPublisher.publishEvent(
                    new StartBattleEvent(userId, opponentId, startBattleResponse)
            );
        } else {
            log.info("用户 {} 确认准备，等待对手 {}", userId, opponentId);
            eventPublisher.publishEvent(
                    new PlayerReadyEvent(userId, opponentId)
            );
        }
    }

//    endregion

    public void discardCardInstances(Long userId, Set<Integer> cardInstanceIds) {
        log.info("用户 {} 弃牌 {}", userId, cardInstanceIds);

        LinkedList<CardInstance> handCardInstances = challengerPlayers.get(userId).getHandCardInstances();
        discardCardInstances(handCardInstances, cardInstanceIds, this.discardDecks);
    }

    public static void discardCardInstances(LinkedList<CardInstance> handCardInstances,
                                            Set<Integer> cardInstanceIds,
                                            Map<String, LinkedList<CardInstance>> discardDecks) {
        Iterator<CardInstance> iterator = handCardInstances.iterator();
        while (iterator.hasNext()) {
            CardInstance cardInstance = iterator.next();
            if (cardInstanceIds.contains(cardInstance.getId())) {
                iterator.remove();
                String level = cardMap.get(cardInstance.getCardId()).getLevel();
                if (LevelEnum.valueOf(level).isKept()) {
                    discardDecks.get(level).add(cardInstance);
                }
            }
        }
    }


    public void award(String battlefield, Long winnerId) {
        log.info("房间 {} 当前回合 {} 战场 {} 颁奖", roomId, battlefield, currentRound);

        LinkedList<CupInstance> cupInstanceList = cupInstances.get(currentRound).getCupInstanceList();
        challengerPlayers.get(winnerId).getCupInstances().add(cupInstanceList.removeFirst());


        if (tempBattlefields.values().stream()
                .allMatch(battlefieldName -> battlefieldName.getCurrentState() == BattleStateEnum.endBattle)) {
            log.info("{} 回合所有战斗结束", currentRound);
            nextRound();
        }

    }


    public void nextRound() {
        log.info("房间 {} 进入下一轮", roomId);

        RoundEnum currentRound = RoundEnum.getByValue(this.currentRound);
        ThrowUtils.throwIf(currentRound == RoundEnum.getLastRound(),
                ErrorCode.PARAMS_ERROR, "已经是最后一轮了");
        this.currentRound = Objects.requireNonNull(
                Objects.requireNonNull(currentRound).getNextRound()
        ).getValue();

        resetBattlefield();
        log.info("房间 {} 进入第 {} 轮", roomId, this.currentRound);
    }


}

