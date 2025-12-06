package com.t0r.sandstormkingbackend.game.challenger.handler;

import cn.hutool.core.io.IoUtil;
import cn.hutool.json.JSONUtil;
import com.opencsv.CSVReader;
import com.t0r.sandstormkingbackend.game.challenger.model.dto.RoomInitRequest;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.*;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.TotalPlayerCountEnum;
import com.t0r.sandstormkingbackend.service.RoomService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Component
public class ChallengerGameManager {

    @javax.annotation.Resource
    private RedisTemplate<String, Object> redisTemplate;

    @javax.annotation.Resource
    private RoomService roomService;

    // 卡牌 ID -> Card
    // TODO 封装一个类专门处理这个数据，包括加载、根据 ID 获取
    public final static Map<Integer, Card> cardMap = new HashMap<>();

    // 人数 -> 战场安排列表 ( 回合数 -> 战场 )
    public final static Map<Integer, List<Map<String, String>>> battlefieldArrangeMap = new HashMap<>();

    // 房间 ID -> RoomGameState
    private final static Map<Long, RoomGameState> roomGameStateMap = new ConcurrentHashMap<>();

    ChallengerGameManager() {
        loadCardMap();
        loadBattlefieldArrange();
    }

    public void discardCardInstances(Long roomId, Long userId, Set<Integer> cardInstanceIds) {
        roomGameStateMap.get(roomId).discardCardInstances(userId, cardInstanceIds);
    }

    public ChallengerPlayer getChallengerPlayer(Long roomId, Long userId) {
        RoomGameState roomGameState = roomGameStateMap.get(roomId);
        return roomGameState.getChallengerPlayers().get(userId);
    }

    public void startGame(RoomInitRequest roomInitRequest) {
        Long roomId = roomInitRequest.getRoomId();
        roomGameStateMap.put(roomId, new RoomGameState(roomInitRequest));
    }

    public LinkedList<CardInstance> buildCardInstances(Long roomId, Long userId, Integer OptionId, Set<Integer> selectedCardInstanceIds) {
        RoomGameState roomGameState = roomGameStateMap.get(roomId);
        roomGameState.buildCardInstances(userId, OptionId, selectedCardInstanceIds);
        return roomGameState.getChallengerPlayers().get(userId).getTempSelectedCardInstances();
    }

    /**
     * @return 对手 ID
     */
    public Long readyBattle(Long roomId, Long userId, String battlefield) {
        return roomGameStateMap.get(roomId).readyBattle(battlefield, userId);
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
                        for (String battlefield : rows.get(i)) {
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
