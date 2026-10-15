package cn.game.board.service;

import cn.game.board.model.avalon.dto.GameRoom;
import cn.game.board.model.avalon.dto.Player;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MissionService {

    // 不同人数的任务人数配置
    private static final Map<Integer, List<Integer>> MISSION_SIZES = Map.of(
            5, List.of(2, 3, 2, 3, 3),
            6, List.of(2, 3, 4, 3, 4),
            7, List.of(2, 3, 3, 4, 4),
            8, List.of(3, 4, 4, 5, 5),
            9, List.of(3, 4, 4, 5, 5),
            10, List.of(3, 4, 4, 5, 5),
            11, List.of(3, 4, 4, 5, 5),
            12, List.of(3, 4, 4, 5, 5)
    );

    /**
     * 获取当前轮次需要的任务人数
     */
    public int getRequiredMissionSize(GameRoom room) {
        List<Integer> sizes = MISSION_SIZES.get(room.getTotalPlayers());
        if (sizes == null || room.getCurrentMissionRound() < 1 || room.getCurrentMissionRound() > sizes.size()) {
            return 0;
        }
        return sizes.get(room.getCurrentMissionRound() - 1);
    }

    /**
     * 检查是否是保护轮（7人及以上，第4轮）
     */
    public boolean isProtectionRound(GameRoom room) {
        return room.getTotalPlayers() >= 7 && room.getCurrentMissionRound() == 4;
    }

    /**
     * 开始新任务
     */
    public void startMission(GameRoom room) {
        synchronized (room) {
            room.setCurrentMissionRound(room.getCurrentMissionRound() + 1);
            room.setSelectedMissionMembers(new HashSet<>());
            room.setMissionProposerSessionId(null);
            room.setVotes(new ConcurrentHashMap<>());
            room.setMissionVotingCompleted(false);
            room.setMissionResult(null);
            room.setMissionFailCount(0);
            room.setLakeLadyLastInspectorSeat(null);

            // 更新领袖（轮换）
            List<Player> players = new ArrayList<>(room.getPlayers().values());
            players.sort(Comparator.comparingInt(Player::getSeatNumber));
            if (players.isEmpty()) {
                log.warn("房间 {} 没有玩家，无法开始任务", room.getRoomNumber());
                return;
            }
            // 确保索引有效
            if (room.getCurrentLeaderIndex() >= players.size() || room.getCurrentLeaderIndex() < 0) {
                room.setCurrentLeaderIndex(0);
            }
            // 使用当前索引作为当前任务的领袖
            Player currentLeader = players.get(room.getCurrentLeaderIndex());
            room.setCurrentLeaderSeat(currentLeader.getSeatNumber());
            // 为下一轮任务准备领袖索引（轮换到下一个玩家）
            room.setCurrentLeaderIndex((room.getCurrentLeaderIndex() + 1) % players.size());
            log.info("房间 {} 开始第 {} 轮任务，领袖是 {} 号", room.getRoomNumber(), room.getCurrentMissionRound(), currentLeader.getSeatNumber());
        }
    }

    /**
     * 玩家加入任务（点击"上车做任务"）
     */
    public void joinMission(GameRoom room, String playerSessionId) {
        synchronized (room) {
            Player player = room.getPlayers().get(playerSessionId);
            if (player == null) {
                throw new RuntimeException("您不在房间中");
            }

            int seatNumber = player.getSeatNumber();
            if (seatNumber == 0) {
                throw new RuntimeException("您尚未选座");
            }

            if (room.getSelectedMissionMembers() == null) {
                room.setSelectedMissionMembers(new HashSet<>());
            }

            // 如果上一轮任务已完成，需要先开始新任务（清空旧数据）
            if (room.isMissionVotingCompleted() && room.getMissionResult() != null) {
                // 这种情况不应该发生，因为应该在调用 joinMission 之前调用 startMission
                // 但为了安全，我们在这里也处理一下
                log.warn("房间 {} 在加入任务时发现上一轮任务已完成，但未开始新任务", room.getRoomNumber());
            }

            int requiredSize = getRequiredMissionSize(room);
            if (room.getSelectedMissionMembers().size() >= requiredSize) {
                throw new RuntimeException("任务人数已满，需要 " + requiredSize + " 人");
            }

            // 如果玩家已经在任务中，不允许重复加入
            if (room.getSelectedMissionMembers().contains(seatNumber)) {
                throw new RuntimeException("您已经在任务中");
            }

            // 将玩家加入任务
            room.getSelectedMissionMembers().add(seatNumber);
            log.info("房间 {} 第 {} 轮任务，玩家 {} 号加入任务，当前人数: {}/{}", 
                    room.getRoomNumber(), room.getCurrentMissionRound(), seatNumber, 
                    room.getSelectedMissionMembers().size(), requiredSize);

            // 初始化投票（如果还没初始化）
            if (room.getVotes() == null) {
                room.setVotes(new ConcurrentHashMap<>());
            }
            room.setMissionVotingCompleted(false);
        }
    }

    /**
     * 玩家退出任务（点击"返回"按钮）
     */
    public void leaveMission(GameRoom room, String playerSessionId) {
        synchronized (room) {
            Player player = room.getPlayers().get(playerSessionId);
            if (player == null) {
                throw new RuntimeException("您不在房间中");
            }

            int seatNumber = player.getSeatNumber();
            if (seatNumber == 0) {
                throw new RuntimeException("您尚未选座");
            }

            if (room.getSelectedMissionMembers() == null) {
                return; // 如果任务成员列表为空，直接返回
            }

            // 如果玩家在任务中，移除玩家
            if (room.getSelectedMissionMembers().remove(seatNumber)) {
                log.info("房间 {} 第 {} 轮任务，玩家 {} 号退出任务，当前人数: {}/{}", 
                        room.getRoomNumber(), room.getCurrentMissionRound(), seatNumber, 
                        room.getSelectedMissionMembers().size(), getRequiredMissionSize(room));
                
                // 如果玩家已经投票，移除投票记录
                if (room.getVotes() != null) {
                    room.getVotes().remove(playerSessionId);
                }
            }
        }
    }

    /**
     * 玩家自己选择是否上车（保留用于其他场景）
     */
    public void toggleMissionMember(GameRoom room, String playerSessionId, boolean joinMission) {
        synchronized (room) {
            Player player = room.getPlayers().get(playerSessionId);
            if (player == null) {
                throw new RuntimeException("您不在房间中");
            }

            int seatNumber = player.getSeatNumber();
            if (seatNumber == 0) {
                throw new RuntimeException("您尚未选座");
            }

            if (room.getSelectedMissionMembers() == null) {
                room.setSelectedMissionMembers(new HashSet<>());
            }

            if (joinMission) {
                int requiredSize = getRequiredMissionSize(room);
                if (room.getSelectedMissionMembers().size() >= requiredSize) {
                    throw new RuntimeException("任务人数已满，需要 " + requiredSize + " 人");
                }
                room.getSelectedMissionMembers().add(seatNumber);
                log.info("房间 {} 第 {} 轮任务，玩家 {} 号上车", room.getRoomNumber(), room.getCurrentMissionRound(), seatNumber);
            } else {
                room.getSelectedMissionMembers().remove(seatNumber);
                log.info("房间 {} 第 {} 轮任务，玩家 {} 号下车", room.getRoomNumber(), room.getCurrentMissionRound(), seatNumber);
            }

            // 初始化投票（如果还没初始化）
            if (room.getVotes() == null) {
                room.setVotes(new ConcurrentHashMap<>());
            }
            room.setMissionVotingCompleted(false);
        }
    }

    /**
     * 投票
     */
    public void vote(GameRoom room, String voterSessionId, String vote) {
        synchronized (room) {
            if (!"success".equals(vote) && !"fail".equals(vote)) {
                throw new RuntimeException("无效的投票: " + vote);
            }

            Player voter = room.getPlayers().get(voterSessionId);
            if (voter == null) {
                throw new RuntimeException("投票者不在房间中");
            }

            // 只有任务成员才能投票
            if (!room.getSelectedMissionMembers().contains(voter.getSeatNumber())) {
                throw new RuntimeException("只有任务成员才能投票");
            }

            // 检查投票权限：好人阵营（除了兰斯洛特（蓝））只能投成功
            String identity = voter.getIdentity();
            if ("fail".equals(vote) && identity != null) {
                // 坏人阵营可以投失败
                Set<String> evilIdentities = Set.of("莫甘娜", "刺客", "莫德雷德的爪牙", "莫德雷德", "奥伯伦", "兰斯洛特（红）");
                // 兰斯洛特（蓝）也可以投失败
                if (!evilIdentities.contains(identity) && !"兰斯洛特（蓝）".equals(identity)) {
                    throw new RuntimeException("好人阵营只能投任务成功");
                }
            }

            room.getVotes().put(voterSessionId, vote);
            log.info("房间 {} 第 {} 轮任务，{} 号玩家（{}）投票: {}", room.getRoomNumber(), room.getCurrentMissionRound(), voter.getSeatNumber(), identity, vote);

            // 检查是否所有人都投票了
            int requiredSize = getRequiredMissionSize(room);
            if (room.getSelectedMissionMembers().size() >= requiredSize
                    && room.getVotes().size() >= requiredSize) {
                calculateMissionResult(room);
            }
        }
    }

    /**
     * 计算任务结果
     */
    private void calculateMissionResult(GameRoom room) {
        int failCount = 0;
        for (String vote : room.getVotes().values()) {
            if ("fail".equals(vote)) {
                failCount++;
            }
        }

        room.setMissionFailCount(failCount);
        boolean isProtection = isProtectionRound(room);
        boolean missionSuccess = isProtection ? failCount < 2 : failCount == 0;
        room.setMissionResult(missionSuccess ? "success" : "fail");
        room.setMissionVotingCompleted(true);

        // 记录任务历史
        if (room.getMissionHistory() == null) {
            room.setMissionHistory(new java.util.ArrayList<>());
        }
        cn.game.board.model.avalon.dto.MissionHistory history = new cn.game.board.model.avalon.dto.MissionHistory(
                room.getCurrentMissionRound(),
                new java.util.HashSet<>(room.getSelectedMissionMembers()),
                room.getMissionResult()
        );
        room.getMissionHistory().add(history);

        // 任务完成后，清空当前任务成员列表，避免重连时误显示结果页面
        // 但保留 missionResult 和 missionVotingCompleted 用于判断是否需要开始新任务
        room.setSelectedMissionMembers(new HashSet<>());
        room.setVotes(new ConcurrentHashMap<>());

        log.info("房间 {} 第 {} 轮任务结果: {} (失败票数: {}, 保护轮: {})", 
                room.getRoomNumber(), room.getCurrentMissionRound(), room.getMissionResult(), failCount, isProtection);
    }

    /**
     * 检查是否可以使用湖中仙女
     * 仅8人及以上，且在第2、3、4轮任务结束后可以使用
     */
    public boolean canUseLakeLady(GameRoom room) {
        // 8人以下不启用湖中仙女
        if (room.getTotalPlayers() < 8) {
            return false;
        }
        int round = room.getCurrentMissionRound();
        return round >= 2 && round <= 4 && room.isMissionVotingCompleted();
    }

    /**
     * 湖中仙女查验玩家
     */
    public void inspectPlayerWithLakeLady(GameRoom room, String inspectorSessionId, int targetSeatNumber) {
        synchronized (room) {
            // 8人以下不启用湖中仙女
            if (room.getTotalPlayers() < 8) {
                throw new RuntimeException("湖中仙女仅在8人及以上游戏中可用");
            }
            
            Player inspector = room.getPlayers().get(inspectorSessionId);
            if (inspector == null) {
                throw new RuntimeException("您不在房间中");
            }

            int inspectorSeat = inspector.getSeatNumber();
            if (room.getLakeLadyHolderSeat() == null || !room.getLakeLadyHolderSeat().equals(inspectorSeat)) {
                throw new RuntimeException("您不是湖中仙女持有者");
            }

            if (!canUseLakeLady(room)) {
                throw new RuntimeException("当前轮次不能使用湖中仙女（只能在第2、3、4轮任务结束后使用）");
            }

            // 检查是否已经使用过湖中仙女（每轮只能使用一次）
            int currentRound = room.getCurrentMissionRound();
            if (room.getLakeLadyInspectionHistory().containsKey(currentRound)) {
                throw new RuntimeException("本轮已经使用过湖中仙女");
            }

            // 检查目标玩家是否曾经持有过湖中仙女
            if (room.getLakeLadyUsedSeats().contains(targetSeatNumber)) {
                throw new RuntimeException("不能查验曾经持有过湖中仙女的玩家");
            }

            // 检查目标玩家是否存在
            Player targetPlayer = room.getPlayers().values().stream()
                    .filter(p -> p.getSeatNumber() == targetSeatNumber)
                    .findFirst()
                    .orElse(null);
            if (targetPlayer == null) {
                throw new RuntimeException("目标玩家不存在");
            }

            // 获取目标玩家的阵营
            String targetIdentity = targetPlayer.getIdentity();
            String targetCamp = getCampByIdentity(targetIdentity);
            room.getLakeLadyInspectionResult().put(targetSeatNumber, targetCamp);
            room.getLakeLadyInspectionHistory().put(currentRound, targetSeatNumber);
            room.getLakeLadyPublicResult().remove(targetSeatNumber);
            room.setLakeLadyLastInspectorSeat(inspectorSeat);
            room.setLakeLadyHolderSeat(targetSeatNumber);
            room.getLakeLadyUsedSeats().add(targetSeatNumber);

            log.info("房间 {} 第 {} 轮，{} 号玩家（湖中仙女持有者）查验 {} 号玩家，阵营: {}", 
                    room.getRoomNumber(), currentRound, inspectorSeat, targetSeatNumber, targetCamp);
        }
    }

    /**
     * 公开湖中仙女查验结果
     */
    public void publishLakeLadyResult(GameRoom room, String inspectorSessionId, int targetSeatNumber, String publicResult) {
        synchronized (room) {
            // 8人以下不启用湖中仙女
            if (room.getTotalPlayers() < 8) {
                throw new RuntimeException("湖中仙女仅在8人及以上游戏中可用");
            }
            
            Player inspector = room.getPlayers().get(inspectorSessionId);
            if (inspector == null) {
                throw new RuntimeException("您不在房间中");
            }

            int inspectorSeat = inspector.getSeatNumber();
            if (room.getLakeLadyHolderSeat() == null || !room.getLakeLadyHolderSeat().equals(inspectorSeat)) {
                throw new RuntimeException("您不是湖中仙女持有者");
            }

            // 检查是否已经查验过该玩家
            if (!room.getLakeLadyInspectionResult().containsKey(targetSeatNumber)) {
                throw new RuntimeException("尚未查验该玩家");
            }

            if (!"good".equals(publicResult) && !"evil".equals(publicResult) && !"unknown".equals(publicResult)) {
                throw new RuntimeException("无效的公开结果");
            }

            room.getLakeLadyPublicResult().put(targetSeatNumber, publicResult);

            // 传递湖中仙女给被查验的玩家
            room.setLakeLadyHolderSeat(targetSeatNumber);
            room.getLakeLadyUsedSeats().add(targetSeatNumber);

            log.info("房间 {} {} 号玩家公开查验结果: {} 号玩家是 {}，湖中仙女已传递给 {} 号", 
                    room.getRoomNumber(), inspectorSeat, targetSeatNumber, publicResult, targetSeatNumber);
        }
    }

    /**
     * 根据身份获取阵营
     */
    private String getCampByIdentity(String identity) {
        if (identity == null) {
            return "unknown";
        }
        Set<String> evilIdentities = Set.of("莫甘娜", "刺客", "莫德雷德的爪牙", "莫德雷德", "奥伯伦", "兰斯洛特（红）");
        return evilIdentities.contains(identity) ? "evil" : "good";
    }

    /**
     * 获取当前领袖的 sessionId
     */
    public String getCurrentLeaderSessionId(GameRoom room) {
        return room.getPlayers().values().stream()
                .filter(p -> p.getSeatNumber() == room.getCurrentLeaderSeat())
                .findFirst()
                .map(Player::getSessionId)
                .orElse(null);
    }
}

