package cn.game.board.model.avalon.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 游戏房间
 */
@Data
@NoArgsConstructor
public class GameRoom {
    private String roomNumber;
    private int totalPlayers;
    private String hostPlayerId;
    private boolean gameStarted = false;
    private Map<String, Player> players = new ConcurrentHashMap<>();
    private Map<String, Player> initialPlayers = new ConcurrentHashMap<>();
    private int gameCount = 0;

    // 需要序列化到 Redis 以便重连时恢复
    private List<String> shuffledIdentities;
    private int currentLeaderSeat;

    // 任务相关状态
    private int currentMissionRound = 0; // 当前任务轮次 (1-5)
    private int currentLeaderIndex = 0; // 当前领袖在玩家列表中的索引
    private Set<Integer> selectedMissionMembers = new java.util.HashSet<>(); // 当前任务选中的成员座位号
    private String missionProposerSessionId; // 提出任务方案的玩家 sessionId
    private Map<String, String> votes = new ConcurrentHashMap<>(); // 投票：sessionId -> "success" 或 "fail"
    private boolean missionVotingCompleted = false; // 任务投票是否完成
    private String missionResult; // "success" 或 "fail"
    private int missionFailCount = 0; // 任务失败票数
    private List<MissionHistory> missionHistory = new java.util.ArrayList<>(); // 任务历史记录

    // 湖中仙女相关状态
    private Integer lakeLadyHolderSeat = null; // 当前持有湖中仙女的玩家座位号
    private Set<Integer> lakeLadyUsedSeats = new java.util.HashSet<>(); // 已经持有过湖中仙女的玩家座位号集合
    private Map<Integer, Integer> lakeLadyInspectionHistory = new java.util.HashMap<>(); // 查验历史：轮次 -> 被查验的座位号
    private Map<Integer, String> lakeLadyInspectionResult = new java.util.HashMap<>(); // 查验结果：被查验座位号 -> "good" 或 "evil"
    private Map<Integer, String> lakeLadyPublicResult = new java.util.HashMap<>(); // 公开的查验结果：被查验座位号 -> "good"、"evil" 或 "unknown"（不透露）
    private Integer lakeLadyLastInspectorSeat = null; // 最近一次使用湖中仙女的玩家座位号

    public GameRoom(String roomNumber, int totalPlayers, String hostSessionId) {
        this.roomNumber = roomNumber;
        this.totalPlayers = totalPlayers;
    }
}
