package com.t0r.sandstormkingbackend.game.challenger.handler;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.json.JSONUtil;
import com.opencsv.CSVReader;
import com.t0r.sandstormkingbackend.exception.ErrorCode;
import com.t0r.sandstormkingbackend.exception.ThrowUtils;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.*;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.BattlefieldEnum;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.LevelEnum;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.RoundEnum;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.TotalPlayerCountEnum;
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
import java.io.InputStreamReader;
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
    public final static Map<Integer, Card> cardMap = new HashMap<>();

    // 人数 -> 战场安排列表 ( 回合数 -> 战场 )
    private final static Map<Integer, List<Map<String, String>>> battlefieldArrangeMap = new HashMap<>();

    // 房间 ID -> RoomDecks
    private final static Map<Long, RoomDecks> roomDecksMap = new ConcurrentHashMap<>();

    // 房间 ID -> 用户 ID -> ChallengerPlayer
    public final static Map<Long, Map<Long, ChallengerPlayer>> challengerPlayersMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        ChallengerHandler challengerHandler = new ChallengerHandler();
        challengerHandler.loadBattlefieldArrange();
        List<Map<String, String>> maps = battlefieldArrangeMap.get(4);
        for (Map<String, String> map : maps) {
            log.info("---------------------------");
            for(Map.Entry<String, String> entry : map.entrySet()) {
                log.info(String.format("%s %s", entry.getKey(), entry.getValue()));
            }
        }

    }

    public void test() {
        Map<String, CupInstanceDeck> cupInstances = roomDecksMap.get(1L).getCupInstances();
        for (Map.Entry<String, CupInstanceDeck> entry : cupInstances.entrySet()) {
            log.info(String.format("%s %s", entry.getKey(), entry.getValue()));
        }

    }

    ChallengerHandler() {
        loadCardMap();
        loadBattlefieldArrange();
    }

    public void initGame(Long roomId, String version, Integer playerCount) {
        log.info("初始化游戏");

        ThrowUtils.throwIf(playerCount < MAX_PLAYER_COUNT,
                ErrorCode.PARAMS_ERROR, "最多只能加入 " + MAX_PLAYER_COUNT + " 人");

        roomDecksMap.put(roomId, new RoomDecks());

        roomDecksMap.get(roomId).setCurrentRound(RoundEnum.getFirstRound().getValue());

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

        loadBattlefield(roomId, playerCount);

    }

    public void loadBattlefield(Long roomId, Integer playerCount) {
        log.info("加载房间 {} 的战场", roomId);

        int battlefieldCount = (playerCount + 1) % 2;

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

    public void loadBattlefieldArrange() {
        log.info("加载战场安排");

        for (TotalPlayerCountEnum totalPlayerCountEnum : TotalPlayerCountEnum.values()) {
            String csvPath = totalPlayerCountEnum.getBattlefieldSchedulePath();
            Integer totalPlayerCount = totalPlayerCountEnum.getValue();

            if (csvPath == null || csvPath.isEmpty()) {
                continue;
            }

            try {
                PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                Resource resource = resolver.getResource("classpath:" + csvPath);
                if (!resource.exists()) {
                    log.warn("战场安排文件 {} 不存在，跳过", csvPath);
                    continue;
                }

                try (
                        InputStreamReader isr = new InputStreamReader(resource.getInputStream(),
                                java.nio.charset.StandardCharsets.UTF_8);
                        CSVReader csvReader = new CSVReader(isr)
                ) {
                    List<String[]> rows = csvReader.readAll();

                    if (rows.size() < 2) {
                        log.warn("战场安排文件 {} 数据不足", csvPath);
                        continue;
                    }

                    // 直接从第 2 行开始解析
                    List<Map<String, String>> maps = new ArrayList<>();
                    for (int i = 1; i <= totalPlayerCount; i++) {
                        Map<String, String> map = new HashMap<>();
                        // TODO 加载能否更优雅
                        int roundIndex = 1;
                        for(String battlefield : rows.get(i)) {
                            map.put(Integer.toString(roundIndex++), battlefield);
                        }
                        maps.add(map);
                    }

                    battlefieldArrangeMap.put(totalPlayerCount, maps);
                    log.info("{}人战场安排表加载完成", totalPlayerCount);
                }

            } catch (Exception e) {
                log.error("加载战场安排表失败: {}", csvPath, e);
            }
        }
    }


}
