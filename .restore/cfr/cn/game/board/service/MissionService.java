/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cn.game.board.model.avalon.dto.GameRoom
 *  cn.game.board.model.avalon.dto.MissionHistory
 *  cn.game.board.model.avalon.dto.Player
 *  cn.game.board.service.MissionService
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Service
 */
package cn.game.board.service;

import cn.game.board.model.avalon.dto.GameRoom;
import cn.game.board.model.avalon.dto.MissionHistory;
import cn.game.board.model.avalon.dto.Player;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MissionService {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(MissionService.class);
    private static final Map<Integer, List<Integer>> MISSION_SIZES = Map.of((Object)5, (Object)List.of((Object)2, (Object)3, (Object)2, (Object)3, (Object)3), (Object)6, (Object)List.of((Object)2, (Object)3, (Object)4, (Object)3, (Object)4), (Object)7, (Object)List.of((Object)2, (Object)3, (Object)3, (Object)4, (Object)4), (Object)8, (Object)List.of((Object)3, (Object)4, (Object)4, (Object)5, (Object)5), (Object)9, (Object)List.of((Object)3, (Object)4, (Object)4, (Object)5, (Object)5), (Object)10, (Object)List.of((Object)3, (Object)4, (Object)4, (Object)5, (Object)5), (Object)11, (Object)List.of((Object)3, (Object)4, (Object)4, (Object)5, (Object)5), (Object)12, (Object)List.of((Object)3, (Object)4, (Object)4, (Object)5, (Object)5));

    public int getRequiredMissionSize(GameRoom room) {
        List sizes = (List)MISSION_SIZES.get(room.getTotalPlayers());
        if (sizes == null || room.getCurrentMissionRound() < 1 || room.getCurrentMissionRound() > sizes.size()) {
            return 0;
        }
        return (Integer)sizes.get(room.getCurrentMissionRound() - 1);
    }

    public boolean isProtectionRound(GameRoom room) {
        return room.getTotalPlayers() >= 7 && room.getCurrentMissionRound() == 4;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void startMission(GameRoom room) {
        GameRoom gameRoom = room;
        synchronized (gameRoom) {
            room.setCurrentMissionRound(room.getCurrentMissionRound() + 1);
            room.setSelectedMissionMembers(new HashSet());
            room.setMissionProposerSessionId(null);
            room.setVotes(new ConcurrentHashMap());
            room.setMissionVotingCompleted(false);
            room.setMissionResult(null);
            room.setMissionFailCount(0);
            room.setLakeLadyLastInspectorSeat(null);
            ArrayList players = new ArrayList(room.getPlayers().values());
            players.sort(Comparator.comparingInt(Player::getSeatNumber));
            if (players.isEmpty()) {
                log.warn("\u623f\u95f4 {} \u6ca1\u6709\u73a9\u5bb6\uff0c\u65e0\u6cd5\u5f00\u59cb\u4efb\u52a1", (Object)room.getRoomNumber());
                return;
            }
            if (room.getCurrentLeaderIndex() >= players.size() || room.getCurrentLeaderIndex() < 0) {
                room.setCurrentLeaderIndex(0);
            }
            Player currentLeader = (Player)players.get(room.getCurrentLeaderIndex());
            room.setCurrentLeaderSeat(currentLeader.getSeatNumber());
            room.setCurrentLeaderIndex((room.getCurrentLeaderIndex() + 1) % players.size());
            log.info("\u623f\u95f4 {} \u5f00\u59cb\u7b2c {} \u8f6e\u4efb\u52a1\uff0c\u9886\u8896\u662f {} \u53f7", new Object[]{room.getRoomNumber(), room.getCurrentMissionRound(), currentLeader.getSeatNumber()});
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void joinMission(GameRoom room, String playerSessionId) {
        GameRoom gameRoom = room;
        synchronized (gameRoom) {
            Player player = (Player)room.getPlayers().get(playerSessionId);
            if (player == null) {
                throw new RuntimeException("\u60a8\u4e0d\u5728\u623f\u95f4\u4e2d");
            }
            int seatNumber = player.getSeatNumber();
            if (seatNumber == 0) {
                throw new RuntimeException("\u60a8\u5c1a\u672a\u9009\u5ea7");
            }
            if (room.getSelectedMissionMembers() == null) {
                room.setSelectedMissionMembers(new HashSet());
            }
            if (room.isMissionVotingCompleted() && room.getMissionResult() != null) {
                log.warn("\u623f\u95f4 {} \u5728\u52a0\u5165\u4efb\u52a1\u65f6\u53d1\u73b0\u4e0a\u4e00\u8f6e\u4efb\u52a1\u5df2\u5b8c\u6210\uff0c\u4f46\u672a\u5f00\u59cb\u65b0\u4efb\u52a1", (Object)room.getRoomNumber());
            }
            int requiredSize = this.getRequiredMissionSize(room);
            if (room.getSelectedMissionMembers().size() >= requiredSize) {
                throw new RuntimeException("\u4efb\u52a1\u4eba\u6570\u5df2\u6ee1\uff0c\u9700\u8981 " + requiredSize + " \u4eba");
            }
            if (room.getSelectedMissionMembers().contains(seatNumber)) {
                throw new RuntimeException("\u60a8\u5df2\u7ecf\u5728\u4efb\u52a1\u4e2d");
            }
            room.getSelectedMissionMembers().add(seatNumber);
            log.info("\u623f\u95f4 {} \u7b2c {} \u8f6e\u4efb\u52a1\uff0c\u73a9\u5bb6 {} \u53f7\u52a0\u5165\u4efb\u52a1\uff0c\u5f53\u524d\u4eba\u6570: {}/{}", new Object[]{room.getRoomNumber(), room.getCurrentMissionRound(), seatNumber, room.getSelectedMissionMembers().size(), requiredSize});
            if (room.getVotes() == null) {
                room.setVotes(new ConcurrentHashMap());
            }
            room.setMissionVotingCompleted(false);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void leaveMission(GameRoom room, String playerSessionId) {
        GameRoom gameRoom = room;
        synchronized (gameRoom) {
            Player player = (Player)room.getPlayers().get(playerSessionId);
            if (player == null) {
                throw new RuntimeException("\u60a8\u4e0d\u5728\u623f\u95f4\u4e2d");
            }
            int seatNumber = player.getSeatNumber();
            if (seatNumber == 0) {
                throw new RuntimeException("\u60a8\u5c1a\u672a\u9009\u5ea7");
            }
            if (room.getSelectedMissionMembers() == null) {
                return;
            }
            if (room.getSelectedMissionMembers().remove(seatNumber)) {
                log.info("\u623f\u95f4 {} \u7b2c {} \u8f6e\u4efb\u52a1\uff0c\u73a9\u5bb6 {} \u53f7\u9000\u51fa\u4efb\u52a1\uff0c\u5f53\u524d\u4eba\u6570: {}/{}", new Object[]{room.getRoomNumber(), room.getCurrentMissionRound(), seatNumber, room.getSelectedMissionMembers().size(), this.getRequiredMissionSize(room)});
                if (room.getVotes() != null) {
                    room.getVotes().remove(playerSessionId);
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void toggleMissionMember(GameRoom room, String playerSessionId, boolean joinMission) {
        GameRoom gameRoom = room;
        synchronized (gameRoom) {
            Player player = (Player)room.getPlayers().get(playerSessionId);
            if (player == null) {
                throw new RuntimeException("\u60a8\u4e0d\u5728\u623f\u95f4\u4e2d");
            }
            int seatNumber = player.getSeatNumber();
            if (seatNumber == 0) {
                throw new RuntimeException("\u60a8\u5c1a\u672a\u9009\u5ea7");
            }
            if (room.getSelectedMissionMembers() == null) {
                room.setSelectedMissionMembers(new HashSet());
            }
            if (joinMission) {
                int requiredSize = this.getRequiredMissionSize(room);
                if (room.getSelectedMissionMembers().size() >= requiredSize) {
                    throw new RuntimeException("\u4efb\u52a1\u4eba\u6570\u5df2\u6ee1\uff0c\u9700\u8981 " + requiredSize + " \u4eba");
                }
                room.getSelectedMissionMembers().add(seatNumber);
                log.info("\u623f\u95f4 {} \u7b2c {} \u8f6e\u4efb\u52a1\uff0c\u73a9\u5bb6 {} \u53f7\u4e0a\u8f66", new Object[]{room.getRoomNumber(), room.getCurrentMissionRound(), seatNumber});
            } else {
                room.getSelectedMissionMembers().remove(seatNumber);
                log.info("\u623f\u95f4 {} \u7b2c {} \u8f6e\u4efb\u52a1\uff0c\u73a9\u5bb6 {} \u53f7\u4e0b\u8f66", new Object[]{room.getRoomNumber(), room.getCurrentMissionRound(), seatNumber});
            }
            if (room.getVotes() == null) {
                room.setVotes(new ConcurrentHashMap());
            }
            room.setMissionVotingCompleted(false);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void vote(GameRoom room, String voterSessionId, String vote) {
        GameRoom gameRoom = room;
        synchronized (gameRoom) {
            Set evilIdentities;
            if (!"success".equals(vote) && !"fail".equals(vote)) {
                throw new RuntimeException("\u65e0\u6548\u7684\u6295\u7968: " + vote);
            }
            Player voter = (Player)room.getPlayers().get(voterSessionId);
            if (voter == null) {
                throw new RuntimeException("\u6295\u7968\u8005\u4e0d\u5728\u623f\u95f4\u4e2d");
            }
            if (!room.getSelectedMissionMembers().contains(voter.getSeatNumber())) {
                throw new RuntimeException("\u53ea\u6709\u4efb\u52a1\u6210\u5458\u624d\u80fd\u6295\u7968");
            }
            String identity = voter.getIdentity();
            if ("fail".equals(vote) && identity != null && !(evilIdentities = Set.of((Object)"\u83ab\u7518\u5a1c", (Object)"\u523a\u5ba2", (Object)"\u83ab\u5fb7\u96f7\u5fb7\u7684\u722a\u7259", (Object)"\u83ab\u5fb7\u96f7\u5fb7", (Object)"\u5965\u4f2f\u4f26", (Object)"\u5170\u65af\u6d1b\u7279\uff08\u7ea2\uff09")).contains(identity) && !"\u5170\u65af\u6d1b\u7279\uff08\u84dd\uff09".equals(identity)) {
                throw new RuntimeException("\u597d\u4eba\u9635\u8425\u53ea\u80fd\u6295\u4efb\u52a1\u6210\u529f");
            }
            room.getVotes().put(voterSessionId, vote);
            log.info("\u623f\u95f4 {} \u7b2c {} \u8f6e\u4efb\u52a1\uff0c{} \u53f7\u73a9\u5bb6\uff08{}\uff09\u6295\u7968: {}", new Object[]{room.getRoomNumber(), room.getCurrentMissionRound(), voter.getSeatNumber(), identity, vote});
            int requiredSize = this.getRequiredMissionSize(room);
            if (room.getSelectedMissionMembers().size() >= requiredSize && room.getVotes().size() >= requiredSize) {
                this.calculateMissionResult(room);
            }
        }
    }

    private void calculateMissionResult(GameRoom room) {
        int failCount = 0;
        for (String vote : room.getVotes().values()) {
            if (!"fail".equals(vote)) continue;
            ++failCount;
        }
        room.setMissionFailCount(failCount);
        boolean isProtection = this.isProtectionRound(room);
        boolean missionSuccess = isProtection ? failCount < 2 : failCount == 0;
        room.setMissionResult(missionSuccess ? "success" : "fail");
        room.setMissionVotingCompleted(true);
        if (room.getMissionHistory() == null) {
            room.setMissionHistory(new ArrayList());
        }
        MissionHistory history = new MissionHistory(room.getCurrentMissionRound(), new HashSet(room.getSelectedMissionMembers()), room.getMissionResult());
        room.getMissionHistory().add(history);
        room.setSelectedMissionMembers(new HashSet());
        room.setVotes(new ConcurrentHashMap());
        log.info("\u623f\u95f4 {} \u7b2c {} \u8f6e\u4efb\u52a1\u7ed3\u679c: {} (\u5931\u8d25\u7968\u6570: {}, \u4fdd\u62a4\u8f6e: {})", new Object[]{room.getRoomNumber(), room.getCurrentMissionRound(), room.getMissionResult(), failCount, isProtection});
    }

    public boolean canUseLakeLady(GameRoom room) {
        if (room.getTotalPlayers() < 8) {
            return false;
        }
        int round = room.getCurrentMissionRound();
        return round >= 2 && round <= 4 && room.isMissionVotingCompleted();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void inspectPlayerWithLakeLady(GameRoom room, String inspectorSessionId, int targetSeatNumber) {
        GameRoom gameRoom = room;
        synchronized (gameRoom) {
            if (room.getTotalPlayers() < 8) {
                throw new RuntimeException("\u6e56\u4e2d\u4ed9\u5973\u4ec5\u57288\u4eba\u53ca\u4ee5\u4e0a\u6e38\u620f\u4e2d\u53ef\u7528");
            }
            Player inspector = (Player)room.getPlayers().get(inspectorSessionId);
            if (inspector == null) {
                throw new RuntimeException("\u60a8\u4e0d\u5728\u623f\u95f4\u4e2d");
            }
            int inspectorSeat = inspector.getSeatNumber();
            if (room.getLakeLadyHolderSeat() == null || !room.getLakeLadyHolderSeat().equals(inspectorSeat)) {
                throw new RuntimeException("\u60a8\u4e0d\u662f\u6e56\u4e2d\u4ed9\u5973\u6301\u6709\u8005");
            }
            if (!this.canUseLakeLady(room)) {
                throw new RuntimeException("\u5f53\u524d\u8f6e\u6b21\u4e0d\u80fd\u4f7f\u7528\u6e56\u4e2d\u4ed9\u5973\uff08\u53ea\u80fd\u5728\u7b2c2\u30013\u30014\u8f6e\u4efb\u52a1\u7ed3\u675f\u540e\u4f7f\u7528\uff09");
            }
            int currentRound = room.getCurrentMissionRound();
            if (room.getLakeLadyInspectionHistory().containsKey(currentRound)) {
                throw new RuntimeException("\u672c\u8f6e\u5df2\u7ecf\u4f7f\u7528\u8fc7\u6e56\u4e2d\u4ed9\u5973");
            }
            if (room.getLakeLadyUsedSeats().contains(targetSeatNumber)) {
                throw new RuntimeException("\u4e0d\u80fd\u67e5\u9a8c\u66fe\u7ecf\u6301\u6709\u8fc7\u6e56\u4e2d\u4ed9\u5973\u7684\u73a9\u5bb6");
            }
            Player targetPlayer = room.getPlayers().values().stream().filter(p -> p.getSeatNumber() == targetSeatNumber).findFirst().orElse(null);
            if (targetPlayer == null) {
                throw new RuntimeException("\u76ee\u6807\u73a9\u5bb6\u4e0d\u5b58\u5728");
            }
            String targetIdentity = targetPlayer.getIdentity();
            String targetCamp = this.getCampByIdentity(targetIdentity);
            room.getLakeLadyInspectionResult().put(targetSeatNumber, targetCamp);
            room.getLakeLadyInspectionHistory().put(currentRound, targetSeatNumber);
            room.getLakeLadyPublicResult().remove(targetSeatNumber);
            room.setLakeLadyLastInspectorSeat(Integer.valueOf(inspectorSeat));
            room.setLakeLadyHolderSeat(Integer.valueOf(targetSeatNumber));
            room.getLakeLadyUsedSeats().add(targetSeatNumber);
            log.info("\u623f\u95f4 {} \u7b2c {} \u8f6e\uff0c{} \u53f7\u73a9\u5bb6\uff08\u6e56\u4e2d\u4ed9\u5973\u6301\u6709\u8005\uff09\u67e5\u9a8c {} \u53f7\u73a9\u5bb6\uff0c\u9635\u8425: {}", new Object[]{room.getRoomNumber(), currentRound, inspectorSeat, targetSeatNumber, targetCamp});
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void publishLakeLadyResult(GameRoom room, String inspectorSessionId, int targetSeatNumber, String publicResult) {
        GameRoom gameRoom = room;
        synchronized (gameRoom) {
            if (room.getTotalPlayers() < 8) {
                throw new RuntimeException("\u6e56\u4e2d\u4ed9\u5973\u4ec5\u57288\u4eba\u53ca\u4ee5\u4e0a\u6e38\u620f\u4e2d\u53ef\u7528");
            }
            Player inspector = (Player)room.getPlayers().get(inspectorSessionId);
            if (inspector == null) {
                throw new RuntimeException("\u60a8\u4e0d\u5728\u623f\u95f4\u4e2d");
            }
            int inspectorSeat = inspector.getSeatNumber();
            if (room.getLakeLadyHolderSeat() == null || !room.getLakeLadyHolderSeat().equals(inspectorSeat)) {
                throw new RuntimeException("\u60a8\u4e0d\u662f\u6e56\u4e2d\u4ed9\u5973\u6301\u6709\u8005");
            }
            if (!room.getLakeLadyInspectionResult().containsKey(targetSeatNumber)) {
                throw new RuntimeException("\u5c1a\u672a\u67e5\u9a8c\u8be5\u73a9\u5bb6");
            }
            if (!("good".equals(publicResult) || "evil".equals(publicResult) || "unknown".equals(publicResult))) {
                throw new RuntimeException("\u65e0\u6548\u7684\u516c\u5f00\u7ed3\u679c");
            }
            room.getLakeLadyPublicResult().put(targetSeatNumber, publicResult);
            room.setLakeLadyHolderSeat(Integer.valueOf(targetSeatNumber));
            room.getLakeLadyUsedSeats().add(targetSeatNumber);
            log.info("\u623f\u95f4 {} {} \u53f7\u73a9\u5bb6\u516c\u5f00\u67e5\u9a8c\u7ed3\u679c: {} \u53f7\u73a9\u5bb6\u662f {}\uff0c\u6e56\u4e2d\u4ed9\u5973\u5df2\u4f20\u9012\u7ed9 {} \u53f7", new Object[]{room.getRoomNumber(), inspectorSeat, targetSeatNumber, publicResult, targetSeatNumber});
        }
    }

    private String getCampByIdentity(String identity) {
        if (identity == null) {
            return "unknown";
        }
        Set evilIdentities = Set.of((Object)"\u83ab\u7518\u5a1c", (Object)"\u523a\u5ba2", (Object)"\u83ab\u5fb7\u96f7\u5fb7\u7684\u722a\u7259", (Object)"\u83ab\u5fb7\u96f7\u5fb7", (Object)"\u5965\u4f2f\u4f26", (Object)"\u5170\u65af\u6d1b\u7279\uff08\u7ea2\uff09");
        return evilIdentities.contains(identity) ? "evil" : "good";
    }

    public String getCurrentLeaderSessionId(GameRoom room) {
        return room.getPlayers().values().stream().filter(p -> p.getSeatNumber() == room.getCurrentLeaderSeat()).findFirst().map(Player::getSessionId).orElse(null);
    }

    @Generated
    public MissionService() {
    }
}

