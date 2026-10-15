package cn.game.board.service;

import cn.game.board.exception.SeatTakenException;
import cn.game.board.model.avalon.dto.IdentityRequest;
import cn.game.board.model.avalon.dto.IdentityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 游戏服务（REST API 使用）
 */
@Service
@RequiredArgsConstructor
public class GameService {

    private final StringRedisTemplate redisTemplate;

    private static final Map<Integer, List<String>> IDENTITIES_MAP = Map.ofEntries(
            Map.entry(5, List.of("梅林", "派西维尔", "亚瑟的忠臣", "莫甘娜", "刺客")),
            Map.entry(6, List.of("梅林", "派西维尔", "亚瑟的忠臣", "莫甘娜", "刺客", "亚瑟的忠臣")),
            Map.entry(7, List.of("梅林", "派西维尔", "亚瑟的忠臣", "莫甘娜", "刺客", "亚瑟的忠臣", "奥伯伦")),
            Map.entry(8, List.of("梅林", "派西维尔", "亚瑟的忠臣", "莫甘娜", "刺客", "亚瑟的忠臣", "亚瑟的忠臣", "莫德雷德的爪牙")),
            Map.entry(9, List.of("梅林", "派西维尔", "亚瑟的忠臣", "莫甘娜", "刺客", "亚瑟的忠臣", "亚瑟的忠臣", "亚瑟的忠臣", "莫德雷德")),
            Map.entry(10, List.of("梅林", "派西维尔", "亚瑟的忠臣", "莫甘娜", "刺客", "亚瑟的忠臣", "亚瑟的忠臣", "亚瑟的忠臣", "莫德雷德", "奥伯伦"))
    );

    private record RoleInfo(String camp, String emoji, String description, String imageUrl) {}

    private static final Map<String, RoleInfo> ROLE_INFO_MAP = Map.of(
            "梅林", new RoleInfo("good", "🧙‍♂️", "能看到所有坏人（莫德雷德除外）。", "img/梅林.webp"),
            "派西维尔", new RoleInfo("good", "🕵️‍♀️", "能看到梅林和莫甘娜（分不清谁是谁）。", "img/派西维尔.webp"),
            "亚瑟的忠臣", new RoleInfo("good", "👑", "没有特殊技能，凭逻辑判断。", "img/亚瑟的忠臣.webp"),
            "莫甘娜", new RoleInfo("evil", "😈", "假扮梅林，迷惑派西维尔。能看到队友（奥伯伦除外）。", "img/莫甘娜.webp"),
            "刺客", new RoleInfo("evil", "🗡️", "好人赢后可刺杀梅林。能看到队友（奥伯伦除外）。", "img/刺客.webp"),
            "奥伯伦", new RoleInfo("evil", "👻", "坏人，但与队友互不相识（梅林能看到他）。", "img/奥伯伦.webp"),
            "莫德雷德的爪牙", new RoleInfo("evil", "👹", "坏人，能看到队友（奥伯伦除外）。", "img/莫德雷德的爪牙.webp"),
            "莫德雷德", new RoleInfo("evil", "💀", "坏人领袖，梅林看不到他。能看到队友（奥伯伦除外）。", "img/莫德雷德.webp")
    );

    private static final RoleInfo UNKNOWN_ROLE = new RoleInfo("good", "❓", "", "/images/roles/unknown.png");

    public IdentityResponse getIdentity(IdentityRequest request) {
        String key = String.format("avalon:game:%d:seat:%d",
                request.getGameNumber(),
                request.getSeatNumber());
        Boolean wasSet = redisTemplate.opsForValue().setIfAbsent(key, "viewed", 24, TimeUnit.HOURS);

        if (wasSet == null || !wasSet) {
            throw new SeatTakenException("此座位已被查看！");
        }

        List<String> baseIdentities = IDENTITIES_MAP.get(request.getPlayersNumber());
        if (baseIdentities == null) {
            throw new IllegalArgumentException("无效的总人数");
        }

        List<String> shuffledIdentities = new ArrayList<>(baseIdentities);
        long seed = request.getGameNumber() + LocalDate.now().toEpochDay();
        Random seededRandom = new Random(seed);
        Collections.shuffle(shuffledIdentities, seededRandom);

        String myIdentity = shuffledIdentities.get(request.getSeatNumber() - 1);
        RoleInfo myRoleInfo = ROLE_INFO_MAP.getOrDefault(myIdentity, UNKNOWN_ROLE);

        List<Integer> seenPlayers = new ArrayList<>();
        String seenTitle = "你什么也没看到";
        String seenSubtitle = "不是先知，胜似先知";
        Set<String> evilTeammates = Set.of("莫甘娜", "刺客", "莫德雷德的爪牙", "莫德雷德");

        switch (myIdentity) {
            case "梅林":
                seenTitle = "你看到了 (坏人):";
                seenSubtitle = "你无法看到 莫德雷德。";
                for (int i = 0; i < shuffledIdentities.size(); i++) {
                    if (Set.of("莫甘娜", "刺客", "奥伯伦", "莫德雷德的爪牙").contains(shuffledIdentities.get(i))) {
                        seenPlayers.add(i + 1);
                    }
                }
                break;
            case "派西维尔":
                seenTitle = "你看到了 (梅林/莫甘娜):";
                seenSubtitle = "他们之中一个是梅林，一个是莫甘娜。";
                for (int i = 0; i < shuffledIdentities.size(); i++) {
                    if (Set.of("梅林", "莫甘娜").contains(shuffledIdentities.get(i))) {
                        seenPlayers.add(i + 1);
                    }
                }
                break;
            case "莫甘娜", "刺客", "莫德雷德的爪牙", "莫德雷德":
                seenTitle = "你看到了 (你的队友):";
                seenSubtitle = "你无法看到 奥伯伦。";
                for (int i = 0; i < shuffledIdentities.size(); i++) {
                    if (evilTeammates.contains(shuffledIdentities.get(i)) && (i + 1) != request.getSeatNumber()) {
                        seenPlayers.add(i + 1);
                    }
                }
                break;
        }

        return IdentityResponse.builder()
                .identity(myIdentity)
                .emoji(myRoleInfo.emoji())
                .camp(myRoleInfo.camp())
                .seenTitle(seenTitle)
                .seenPlayers(seenPlayers)
                .seenSubtitle(seenSubtitle)
                .imageUrl(myRoleInfo.imageUrl())
                .build();
    }
}
