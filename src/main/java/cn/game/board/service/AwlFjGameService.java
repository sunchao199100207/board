package cn.game.board.service;

import cn.game.board.model.avalon.dto.GameRoom;
import cn.game.board.model.avalon.dto.IdentityResponse;
import cn.game.board.model.avalon.dto.Player;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 阿瓦隆游戏逻辑服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AwlFjGameService {

    private final SimpMessagingTemplate messagingTemplate;

    private static final Map<Integer, List<String>> IDENTITIES_MAP = Map.ofEntries(
            Map.entry(5, List.of("梅林", "派西维尔", "亚瑟的忠臣", "莫甘娜", "刺客")),
            Map.entry(6, List.of("梅林", "派西维尔", "亚瑟的忠臣", "莫甘娜", "刺客", "亚瑟的忠臣")),
            Map.entry(7, List.of("梅林", "派西维尔", "亚瑟的忠臣", "莫甘娜", "刺客", "亚瑟的忠臣", "奥伯伦")),
            Map.entry(8, List.of("梅林", "派西维尔", "亚瑟的忠臣", "莫甘娜", "刺客", "亚瑟的忠臣", "亚瑟的忠臣", "莫德雷德的爪牙")),
            Map.entry(9, List.of("梅林", "派西维尔", "亚瑟的忠臣", "莫甘娜", "刺客", "亚瑟的忠臣", "亚瑟的忠臣", "亚瑟的忠臣", "莫德雷德")),
            Map.entry(10, List.of("梅林", "派西维尔", "亚瑟的忠臣", "莫甘娜", "刺客", "亚瑟的忠臣", "亚瑟的忠臣", "亚瑟的忠臣", "莫德雷德", "奥伯伦")),
            Map.entry(11, List.of("梅林", "派西维尔", "亚瑟的忠臣", "亚瑟的忠臣", "亚瑟的忠臣", "亚瑟的忠臣", "兰斯洛特（蓝）", "莫德雷德", "莫甘娜", "刺客", "兰斯洛特（红）")),
            Map.entry(12, List.of("梅林", "派西维尔", "亚瑟的忠臣", "亚瑟的忠臣", "亚瑟的忠臣", "亚瑟的忠臣", "兰斯洛特（蓝）", "莫德雷德", "莫甘娜", "刺客", "奥伯伦", "兰斯洛特（红）"))
    );

    private record RoleInfo(String camp, String emoji, String description, String imageUrl) {}

    private static final Map<String, RoleInfo> ROLE_INFO_MAP = Map.ofEntries(
            Map.entry("梅林", new RoleInfo("good", "🧙‍♂️", "能看到所有坏人（莫德雷德除外）。", "img/new/梅林.jpg")),
            Map.entry("派西维尔", new RoleInfo("good", "🕵️‍♀️", "能看到梅林和莫甘娜。", "img/new/派西维尔.jpg")),
            Map.entry("亚瑟的忠臣", new RoleInfo("good", "👑", "没有特殊技能。", "img/new/亚瑟的忠臣.jpg")),
            Map.entry("莫甘娜", new RoleInfo("evil", "😈", "假扮梅林。能看到队友（奥伯伦除外）。", "img/new/莫甘娜.jpg")),
            Map.entry("刺客", new RoleInfo("evil", "🗡️", "刺杀梅林。能看到队友（奥伯伦除外）。", "img/new/刺客.jpg")),
            Map.entry("莫德雷德的爪牙", new RoleInfo("evil", "👹", "坏人。能看到队友（奥伯伦除外）。", "img/new/莫德雷德的爪牙.jpg")),
            Map.entry("莫德雷德", new RoleInfo("evil", "💀", "坏人领袖，梅林看不到。能看到队友（奥伯伦除外）。", "img/new/莫德雷德.jpg")),
            Map.entry("奥伯伦", new RoleInfo("evil", "👻", "坏人，与队友互不相识。", "img/new/奥伯伦.jpg")),
            Map.entry("兰斯洛特（蓝）", new RoleInfo("good", "🤺", "好人，只能看到红兰。", "img/new/兰斯洛特（蓝）.jpg")),
            Map.entry("兰斯洛特（红）", new RoleInfo("evil", "💔", "坏人，只能看到蓝兰。（梅林和其他坏人可见）", "img/new/兰斯洛特（红）.jpg"))
    );

    private static final RoleInfo UNKNOWN_ROLE = new RoleInfo("good", "❓", "", "img/未知.webp");

    /**
     * 检查游戏是否可以开始
     */
    public void checkGameStart(GameRoom room) {
        if (room == null || room.isGameStarted()) return;

        synchronized (room) {
            if (room.isGameStarted()) return;
            if (room.getPlayers().size() != room.getTotalPlayers()) return;
            boolean allReady = room.getPlayers().values().stream().allMatch(Player::isReady);
            if (!allReady) return;

            room.setGameStarted(true);
            room.setGameCount(room.getGameCount() + 1);

            List<String> baseIdentities = IDENTITIES_MAP.get(room.getTotalPlayers());
            if (baseIdentities == null) {
                handleInvalidConfig(room, "无效人数配置 " + room.getTotalPlayers());
                return;
            }

            List<String> shuffledIdentities = new ArrayList<>(baseIdentities);
            long seed = Long.parseLong(room.getRoomNumber()) + LocalDate.now().toEpochDay() + room.getGameCount() + System.nanoTime();
            Collections.shuffle(shuffledIdentities, new Random(seed));
            room.setShuffledIdentities(new ArrayList<>(shuffledIdentities));

            int leaderSeat = new Random(seed).nextInt(room.getTotalPlayers()) + 1;
            room.setCurrentLeaderSeat(leaderSeat);

            List<Player> players = new ArrayList<>(room.getPlayers().values());
            players.sort(Comparator.comparingInt(Player::getSeatNumber));

            List<String> assignedIdentities = buildAssignedIdentities(players, shuffledIdentities);
            if (assignedIdentities == null) {
                handleInvalidConfig(room, "身份分配失败");
                return;
            }
            room.setShuffledIdentities(new ArrayList<>(assignedIdentities));
            
            // 初始化领袖索引
            for (int i = 0; i < players.size(); i++) {
                if (players.get(i).getSeatNumber() == leaderSeat) {
                    room.setCurrentLeaderIndex(i);
                    break;
                }
            }

            // 初始化湖中仙女持有者：第一位领袖的右手边玩家（仅8人及以上）
            // 如果领袖是 N 号，右手边是 (N-1) 号，如果 N=1，则右手边是 totalPlayers 号
            if (room.getTotalPlayers() >= 8) {
                int lakeLadyHolderSeat = leaderSeat - 1;
                if (lakeLadyHolderSeat < 1) {
                    lakeLadyHolderSeat = room.getTotalPlayers();
                }
                room.setLakeLadyHolderSeat(lakeLadyHolderSeat);
                room.getLakeLadyUsedSeats().add(lakeLadyHolderSeat);
                room.setLakeLadyLastInspectorSeat(null);
                log.info("房间 {} 游戏开始 (第 {} 局, {}人, 领袖: {}号, 湖中仙女: {}号)", 
                        room.getRoomNumber(), room.getGameCount(), room.getTotalPlayers(), leaderSeat, lakeLadyHolderSeat);
            } else {
                // 8人以下不启用湖中仙女
                room.setLakeLadyHolderSeat(null);
                room.getLakeLadyUsedSeats().clear();
                room.setLakeLadyLastInspectorSeat(null);
                log.info("房间 {} 游戏开始 (第 {} 局, {}人, 领袖: {}号, 无湖中仙女)", 
                        room.getRoomNumber(), room.getGameCount(), room.getTotalPlayers(), leaderSeat);
            }

            for (int i = 0; i < players.size(); i++) {
                Player player = players.get(i);
                String identity = assignedIdentities.get(i);
                player.setIdentity(identity);
                IdentityResponse payload = buildIdentityResponse(player.getSeatNumber(), identity, assignedIdentities, leaderSeat);
                messagingTemplate.convertAndSendToUser(player.getSessionId(), "/queue/game-start", payload);
            }

            room.getInitialPlayers().clear();
            room.getInitialPlayers().putAll(room.getPlayers());
        }
    }

    private List<String> buildAssignedIdentities(List<Player> players, List<String> shuffledIdentities) {
        List<String> remaining = new ArrayList<>(shuffledIdentities);
        List<String> assigned = new ArrayList<>(Collections.nCopies(players.size(), null));

        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            String preferred = player.getPreferredRole();
            if (preferred == null || preferred.isBlank()) {
                continue;
            }
            if (remaining.remove(preferred)) {
                assigned.set(i, preferred);
                log.info("为玩家 {} 强制分配身份 {}", player.getPlayerId(), preferred);
            } else {
                log.warn("玩家 {} 请求的身份 {} 不可用或数量不足，忽略强制分配", player.getPlayerId(), preferred);
            }
        }

        int cursor = 0;
        for (int i = 0; i < assigned.size(); i++) {
            if (assigned.get(i) == null) {
                if (cursor >= remaining.size()) {
                    return null;
                }
                assigned.set(i, remaining.get(cursor++));
            }
        }
        return assigned;
    }

    /**
     * 向重连玩家发送身份信息
     */
    public void sendIdentityToPlayer(GameRoom room, Player player, String targetSessionId) {
        if (room == null) {
            log.error("无法发送身份：房间为 null");
            messagingTemplate.convertAndSendToUser(targetSessionId, "/queue/errors", Map.of("error", "无法恢复身份信息：房间不存在"));
            return;
        }
        if (!room.isGameStarted()) {
            log.error("无法发送身份给玩家 {}: 游戏未开始", player != null ? player.getPlayerId() : "未知");
            messagingTemplate.convertAndSendToUser(targetSessionId, "/queue/errors", Map.of("error", "无法恢复身份信息：游戏未开始"));
            return;
        }
        if (player == null || player.getIdentity() == null) {
            log.error("无法发送身份给玩家: 玩家或身份为 null");
            messagingTemplate.convertAndSendToUser(targetSessionId, "/queue/errors", Map.of("error", "无法恢复身份信息：玩家身份为空"));
            return;
        }
        if (room.getShuffledIdentities() == null || room.getCurrentLeaderSeat() <= 0) {
            log.error("无法发送身份给玩家 {}: 游戏信息不完整", player.getPlayerId());
            messagingTemplate.convertAndSendToUser(targetSessionId, "/queue/errors", Map.of("error", "无法恢复身份信息：游戏信息丢失"));
            return;
        }

        IdentityResponse payload = buildIdentityResponse(player.getSeatNumber(), player.getIdentity(), room.getShuffledIdentities(), room.getCurrentLeaderSeat());
        messagingTemplate.convertAndSendToUser(targetSessionId, "/queue/game-start", payload);
    }

    /**
     * 构建玩家身份响应
     */
    private IdentityResponse buildIdentityResponse(int mySeatNumber, String myIdentity, List<String> shuffledIdentities, int leaderSeat) {
        RoleInfo myRoleInfo = ROLE_INFO_MAP.getOrDefault(myIdentity, UNKNOWN_ROLE);
        List<Integer> seenPlayers = new ArrayList<>();
        String seenTitle = "你什么也没看到";
        String seenSubtitle = "";

        boolean mordredInGame = shuffledIdentities.contains("莫德雷德");
        boolean oberonInGame = shuffledIdentities.contains("奥伯伦");
        int redLancelotSeat = -1, blueLancelotSeat = -1;

        for (int i = 0; i < shuffledIdentities.size(); i++) {
            String role = shuffledIdentities.get(i);
            if (role.equals("兰斯洛特（红）")) redLancelotSeat = i + 1;
            if (role.equals("兰斯洛特（蓝）")) blueLancelotSeat = i + 1;
        }

        Set<String> merlinSees = Set.of("莫甘娜", "刺客", "奥伯伦", "莫德雷德的爪牙", "兰斯洛特（红）");
        Set<String> allEvilRoles = Set.of("莫甘娜", "刺客", "莫德雷德的爪牙", "莫德雷德", "兰斯洛特（红）");

        switch (myIdentity) {
            case "梅林":
                seenTitle = "你看到了 (坏人):";
                for (int i = 0; i < shuffledIdentities.size(); i++) {
                    if (merlinSees.contains(shuffledIdentities.get(i))) {
                        seenPlayers.add(i + 1);
                    }
                }
                seenSubtitle = mordredInGame ? "你无法看到 莫德雷德。" : "（本局没有莫德雷德）";
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
                for (int i = 0; i < shuffledIdentities.size(); i++) {
                    String role = shuffledIdentities.get(i);
                    int seat = i + 1;
                    if (allEvilRoles.contains(role) && seat != mySeatNumber) {
                        seenPlayers.add(seat);
                    }
                }
                seenSubtitle = oberonInGame ? "你无法看到 奥伯伦。" : "（本局没有奥伯伦）";
                break;
            case "兰斯洛特（蓝）":
                if (redLancelotSeat > 0) {
                    seenTitle = "你看到了 (红兰斯洛特):";
                    seenSubtitle = "他是你的宿敌。";
                    seenPlayers.add(redLancelotSeat);
                } else {
                    seenTitle = "你什么也没看到";
                    seenSubtitle = "本局没有红兰斯洛特。";
                }
                break;
            case "兰斯洛特（红）":
                if (blueLancelotSeat > 0) {
                    seenTitle = "你看到了 (蓝兰斯洛特):";
                    seenSubtitle = "他是你的对手。";
                    seenPlayers.add(blueLancelotSeat);
                } else {
                    seenTitle = "你什么也没看到";
                    seenSubtitle = "本局没有蓝兰斯洛特。";
                }
                break;
            default:
                seenSubtitle = "不是先知，胜似先知";
                break;
        }

        if (seenPlayers.isEmpty() && seenTitle.equals("你什么也没看到") && seenSubtitle.isEmpty()) {
            seenSubtitle = "不是先知，胜似先知";
        }

        return IdentityResponse.builder()
                .identity(myIdentity)
                .emoji(myRoleInfo.emoji())
                .camp(myRoleInfo.camp())
                .seenTitle(seenTitle)
                .seenPlayers(seenPlayers)
                .seenSubtitle(seenSubtitle)
                .imageUrl(myRoleInfo.imageUrl())
                .leaderSeat(leaderSeat)
                .build();
    }

    /**
     * 重置游戏状态
     */
    public void resetGame(GameRoom room, String requestingPlayerId) {
        if (room == null) {
            log.warn("尝试重置 null 房间");
            return;
        }

        String roomNumber = room.getRoomNumber();
        synchronized (room) {
            if (room.isGameStarted()) {
                log.info("房间 {} 游戏状态重置 (玩家 {})", roomNumber, requestingPlayerId);
                room.setGameStarted(false);
                room.getInitialPlayers().clear();
                room.getPlayers().values().forEach(player -> {
                    player.setReady(false);
                    player.setIdentity(null);
                });
                room.setShuffledIdentities(null);
                room.setCurrentLeaderSeat(0);
                room.setCurrentLeaderIndex(0);
                
                // 重置任务相关状态
                room.setCurrentMissionRound(0);
                room.setSelectedMissionMembers(new java.util.HashSet<>());
                room.setMissionProposerSessionId(null);
                room.setVotes(new java.util.concurrent.ConcurrentHashMap<>());
                room.setMissionVotingCompleted(false);
                room.setMissionResult(null);
                room.setMissionFailCount(0);
                room.setMissionHistory(new java.util.ArrayList<>());
                
                // 重置湖中仙女相关状态
                room.setLakeLadyHolderSeat(null);
                room.setLakeLadyUsedSeats(new java.util.HashSet<>());
                room.setLakeLadyInspectionHistory(new java.util.HashMap<>());
                room.setLakeLadyInspectionResult(new java.util.HashMap<>());
                room.setLakeLadyPublicResult(new java.util.HashMap<>());
                room.setLakeLadyLastInspectorSeat(null);
            } else {
                log.debug("房间 {} 无需重复重置", roomNumber);
            }
        }
    }

    /**
     * 处理无效配置
     */
    private void handleInvalidConfig(GameRoom room, String errorMessage) {
        log.error("房间 {} 配置错误: {}", room.getRoomNumber(), errorMessage);
        room.setGameStarted(false);
        room.getPlayers().keySet().forEach(sid ->
                messagingTemplate.convertAndSendToUser(sid, "/queue/errors", Map.of("error", errorMessage))
        );
    }
}
