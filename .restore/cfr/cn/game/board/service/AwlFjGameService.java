/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cn.game.board.model.avalon.dto.GameRoom
 *  cn.game.board.model.avalon.dto.IdentityResponse
 *  cn.game.board.model.avalon.dto.Player
 *  cn.game.board.service.AwlFjGameService
 *  cn.game.board.service.AwlFjGameService$RoleInfo
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.messaging.simp.SimpMessagingTemplate
 *  org.springframework.stereotype.Service
 */
package cn.game.board.service;

import cn.game.board.model.avalon.dto.GameRoom;
import cn.game.board.model.avalon.dto.IdentityResponse;
import cn.game.board.model.avalon.dto.Player;
import cn.game.board.service.AwlFjGameService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class AwlFjGameService {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(AwlFjGameService.class);
    private final SimpMessagingTemplate messagingTemplate;
    private static final Map<Integer, List<String>> IDENTITIES_MAP = Map.ofEntries((Map.Entry[])new Map.Entry[]{Map.entry((Object)5, (Object)List.of((Object)"\u6885\u6797", (Object)"\u6d3e\u897f\u7ef4\u5c14", (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3", (Object)"\u83ab\u7518\u5a1c", (Object)"\u523a\u5ba2")), Map.entry((Object)6, (Object)List.of((Object)"\u6885\u6797", (Object)"\u6d3e\u897f\u7ef4\u5c14", (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3", (Object)"\u83ab\u7518\u5a1c", (Object)"\u523a\u5ba2", (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3")), Map.entry((Object)7, (Object)List.of((Object)"\u6885\u6797", (Object)"\u6d3e\u897f\u7ef4\u5c14", (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3", (Object)"\u83ab\u7518\u5a1c", (Object)"\u523a\u5ba2", (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3", (Object)"\u5965\u4f2f\u4f26")), Map.entry((Object)8, (Object)List.of((Object)"\u6885\u6797", (Object)"\u6d3e\u897f\u7ef4\u5c14", (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3", (Object)"\u83ab\u7518\u5a1c", (Object)"\u523a\u5ba2", (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3", (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3", (Object)"\u83ab\u5fb7\u96f7\u5fb7\u7684\u722a\u7259")), Map.entry((Object)9, (Object)List.of((Object)"\u6885\u6797", (Object)"\u6d3e\u897f\u7ef4\u5c14", (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3", (Object)"\u83ab\u7518\u5a1c", (Object)"\u523a\u5ba2", (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3", (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3", (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3", (Object)"\u83ab\u5fb7\u96f7\u5fb7")), Map.entry((Object)10, (Object)List.of((Object)"\u6885\u6797", (Object)"\u6d3e\u897f\u7ef4\u5c14", (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3", (Object)"\u83ab\u7518\u5a1c", (Object)"\u523a\u5ba2", (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3", (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3", (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3", (Object)"\u83ab\u5fb7\u96f7\u5fb7", (Object)"\u5965\u4f2f\u4f26")), Map.entry((Object)11, (Object)List.of((Object[])new String[]{"\u6885\u6797", "\u6d3e\u897f\u7ef4\u5c14", "\u4e9a\u745f\u7684\u5fe0\u81e3", "\u4e9a\u745f\u7684\u5fe0\u81e3", "\u4e9a\u745f\u7684\u5fe0\u81e3", "\u4e9a\u745f\u7684\u5fe0\u81e3", "\u5170\u65af\u6d1b\u7279\uff08\u84dd\uff09", "\u83ab\u5fb7\u96f7\u5fb7", "\u83ab\u7518\u5a1c", "\u523a\u5ba2", "\u5170\u65af\u6d1b\u7279\uff08\u7ea2\uff09"})), Map.entry((Object)12, (Object)List.of((Object[])new String[]{"\u6885\u6797", "\u6d3e\u897f\u7ef4\u5c14", "\u4e9a\u745f\u7684\u5fe0\u81e3", "\u4e9a\u745f\u7684\u5fe0\u81e3", "\u4e9a\u745f\u7684\u5fe0\u81e3", "\u4e9a\u745f\u7684\u5fe0\u81e3", "\u5170\u65af\u6d1b\u7279\uff08\u84dd\uff09", "\u83ab\u5fb7\u96f7\u5fb7", "\u83ab\u7518\u5a1c", "\u523a\u5ba2", "\u5965\u4f2f\u4f26", "\u5170\u65af\u6d1b\u7279\uff08\u7ea2\uff09"}))});
    private static final Map<String, RoleInfo> ROLE_INFO_MAP = Map.ofEntries((Map.Entry[])new Map.Entry[]{Map.entry((Object)"\u6885\u6797", (Object)new RoleInfo("good", "\ud83e\uddd9\u200d\u2642\ufe0f", "\u80fd\u770b\u5230\u6240\u6709\u574f\u4eba\uff08\u83ab\u5fb7\u96f7\u5fb7\u9664\u5916\uff09\u3002", "img/new/\u6885\u6797.jpg")), Map.entry((Object)"\u6d3e\u897f\u7ef4\u5c14", (Object)new RoleInfo("good", "\ud83d\udd75\ufe0f\u200d\u2640\ufe0f", "\u80fd\u770b\u5230\u6885\u6797\u548c\u83ab\u7518\u5a1c\u3002", "img/new/\u6d3e\u897f\u7ef4\u5c14.jpg")), Map.entry((Object)"\u4e9a\u745f\u7684\u5fe0\u81e3", (Object)new RoleInfo("good", "\ud83d\udc51", "\u6ca1\u6709\u7279\u6b8a\u6280\u80fd\u3002", "img/new/\u4e9a\u745f\u7684\u5fe0\u81e3.jpg")), Map.entry((Object)"\u83ab\u7518\u5a1c", (Object)new RoleInfo("evil", "\ud83d\ude08", "\u5047\u626e\u6885\u6797\u3002\u80fd\u770b\u5230\u961f\u53cb\uff08\u5965\u4f2f\u4f26\u9664\u5916\uff09\u3002", "img/new/\u83ab\u7518\u5a1c.jpg")), Map.entry((Object)"\u523a\u5ba2", (Object)new RoleInfo("evil", "\ud83d\udde1\ufe0f", "\u523a\u6740\u6885\u6797\u3002\u80fd\u770b\u5230\u961f\u53cb\uff08\u5965\u4f2f\u4f26\u9664\u5916\uff09\u3002", "img/new/\u523a\u5ba2.jpg")), Map.entry((Object)"\u83ab\u5fb7\u96f7\u5fb7\u7684\u722a\u7259", (Object)new RoleInfo("evil", "\ud83d\udc79", "\u574f\u4eba\u3002\u80fd\u770b\u5230\u961f\u53cb\uff08\u5965\u4f2f\u4f26\u9664\u5916\uff09\u3002", "img/new/\u83ab\u5fb7\u96f7\u5fb7\u7684\u722a\u7259.jpg")), Map.entry((Object)"\u83ab\u5fb7\u96f7\u5fb7", (Object)new RoleInfo("evil", "\ud83d\udc80", "\u574f\u4eba\u9886\u8896\uff0c\u6885\u6797\u770b\u4e0d\u5230\u3002\u80fd\u770b\u5230\u961f\u53cb\uff08\u5965\u4f2f\u4f26\u9664\u5916\uff09\u3002", "img/new/\u83ab\u5fb7\u96f7\u5fb7.jpg")), Map.entry((Object)"\u5965\u4f2f\u4f26", (Object)new RoleInfo("evil", "\ud83d\udc7b", "\u574f\u4eba\uff0c\u4e0e\u961f\u53cb\u4e92\u4e0d\u76f8\u8bc6\u3002", "img/new/\u5965\u4f2f\u4f26.jpg")), Map.entry((Object)"\u5170\u65af\u6d1b\u7279\uff08\u84dd\uff09", (Object)new RoleInfo("good", "\ud83e\udd3a", "\u597d\u4eba\uff0c\u53ea\u80fd\u770b\u5230\u7ea2\u5170\u3002", "img/new/\u5170\u65af\u6d1b\u7279\uff08\u84dd\uff09.jpg")), Map.entry((Object)"\u5170\u65af\u6d1b\u7279\uff08\u7ea2\uff09", (Object)new RoleInfo("evil", "\ud83d\udc94", "\u574f\u4eba\uff0c\u53ea\u80fd\u770b\u5230\u84dd\u5170\u3002\uff08\u6885\u6797\u548c\u5176\u4ed6\u574f\u4eba\u53ef\u89c1\uff09", "img/new/\u5170\u65af\u6d1b\u7279\uff08\u7ea2\uff09.jpg"))});
    private static final RoleInfo UNKNOWN_ROLE = new RoleInfo("good", "\u2753", "", "img/\u672a\u77e5.webp");

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void checkGameStart(GameRoom room) {
        if (room == null || room.isGameStarted()) {
            return;
        }
        GameRoom gameRoom = room;
        synchronized (gameRoom) {
            if (room.isGameStarted()) {
                return;
            }
            if (room.getPlayers().size() != room.getTotalPlayers()) {
                return;
            }
            boolean allReady = room.getPlayers().values().stream().allMatch(Player::isReady);
            if (!allReady) {
                return;
            }
            log.info("\u623f\u95f4 {} \u6e38\u620f\u5f00\u59cb\uff0c\u5c40\u6570: {}", (Object)room.getRoomNumber(), (Object)(room.getGameCount() + 1));
            room.setGameStarted(true);
            room.setGameCount(room.getGameCount() + 1);
            List baseIdentities = (List)IDENTITIES_MAP.get(room.getTotalPlayers());
            if (baseIdentities == null) {
                this.handleInvalidConfig(room, "\u65e0\u6548\u4eba\u6570\u914d\u7f6e " + room.getTotalPlayers());
                return;
            }
            ArrayList shuffledIdentities = new ArrayList(baseIdentities);
            long seed = Long.parseLong(room.getRoomNumber()) + LocalDate.now().toEpochDay() + (long)room.getGameCount() + System.nanoTime();
            Collections.shuffle(shuffledIdentities, new Random(seed));
            int leaderSeat = new Random(seed).nextInt(room.getTotalPlayers()) + 1;
            room.setCurrentLeaderSeat(leaderSeat);
            ArrayList players = new ArrayList(room.getPlayers().values());
            players.sort(Comparator.comparingInt(Player::getSeatNumber));
            ArrayList<Object> assignedIdentities = new ArrayList<Object>(Collections.nCopies(players.size(), null));
            ArrayList remainingIdentities = new ArrayList(shuffledIdentities);
            int i = 0;
            while (i < players.size()) {
                Player player = (Player)players.get(i);
                String preferredRole = player.getPreferredRole();
                if (preferredRole != null && remainingIdentities.contains(preferredRole)) {
                    remainingIdentities.remove(preferredRole);
                    assignedIdentities.set(i, preferredRole);
                    log.info("\u4e3a\u73a9\u5bb6 {} \u5f3a\u5236\u5206\u914d\u8eab\u4efd {}", (Object)player.getPlayerId(), (Object)preferredRole);
                } else if (preferredRole != null) {
                    log.warn("\u73a9\u5bb6 {} \u8bf7\u6c42\u7684\u8eab\u4efd {} \u4e0d\u53ef\u7528\u6216\u6570\u91cf\u4e0d\u8db3\uff0c\u5ffd\u7565\u5f3a\u5236\u5206\u914d", (Object)player.getPlayerId(), (Object)preferredRole);
                }
                ++i;
            }
            Iterator remainingIterator = remainingIdentities.iterator();
            int i2 = 0;
            while (i2 < players.size()) {
                if (assignedIdentities.get(i2) == null) {
                    if (!remainingIterator.hasNext()) {
                        log.error("\u8eab\u4efd\u5206\u914d\u51fa\u73b0\u5f02\u5e38\uff1a\u5269\u4f59\u8eab\u4efd\u4e0d\u8db3");
                        this.handleInvalidConfig(room, "\u8eab\u4efd\u5206\u914d\u5931\u8d25");
                        return;
                    }
                    String identity = (String)remainingIterator.next();
                    assignedIdentities.set(i2, identity);
                }
                ++i2;
            }
            room.setShuffledIdentities(new ArrayList<Object>(assignedIdentities));
            i2 = 0;
            while (i2 < players.size()) {
                if (((Player)players.get(i2)).getSeatNumber() == leaderSeat) {
                    room.setCurrentLeaderIndex(i2);
                    break;
                }
                ++i2;
            }
            if (room.getTotalPlayers() >= 8) {
                int lakeLadyHolderSeat = leaderSeat - 1;
                if (lakeLadyHolderSeat < 1) {
                    lakeLadyHolderSeat = room.getTotalPlayers();
                }
                room.setLakeLadyHolderSeat(Integer.valueOf(lakeLadyHolderSeat));
                room.getLakeLadyUsedSeats().add(lakeLadyHolderSeat);
                room.setLakeLadyLastInspectorSeat(null);
                log.info("\u623f\u95f4 {} \u6e38\u620f\u5f00\u59cb\uff0c\u9886\u8896\u662f {} \u53f7\uff0c\u6e56\u4e2d\u4ed9\u5973\u6301\u6709\u8005\u662f {} \u53f7", new Object[]{room.getRoomNumber(), leaderSeat, lakeLadyHolderSeat});
            } else {
                room.setLakeLadyHolderSeat(null);
                room.getLakeLadyUsedSeats().clear();
                room.setLakeLadyLastInspectorSeat(null);
                log.info("\u623f\u95f4 {} \u6e38\u620f\u5f00\u59cb\uff0c\u9886\u8896\u662f {} \u53f7\uff0c\u4eba\u6570 {} \u4eba\uff0c\u4e0d\u542f\u7528\u6e56\u4e2d\u4ed9\u5973", new Object[]{room.getRoomNumber(), leaderSeat, room.getTotalPlayers()});
            }
            i2 = 0;
            while (i2 < players.size()) {
                Player player = (Player)players.get(i2);
                String identity = (String)assignedIdentities.get(i2);
                player.setIdentity(identity);
                IdentityResponse payload = this.buildIdentityResponse(player.getSeatNumber(), identity, assignedIdentities, leaderSeat);
                this.messagingTemplate.convertAndSendToUser(player.getSessionId(), "/queue/game-start", (Object)payload);
                ++i2;
            }
            room.getInitialPlayers().clear();
            room.getInitialPlayers().putAll(room.getPlayers());
        }
    }

    public void sendIdentityToPlayer(GameRoom room, Player player, String targetSessionId) {
        log.info("[\u53d1\u9001\u8eab\u4efd] \u5f00\u59cb\u53d1\u9001\u8eab\u4efd\u4fe1\u606f: playerId={}, sessionId={}, seatNumber={}, identity={}", new Object[]{player != null ? player.getPlayerId() : "null", targetSessionId, player != null ? player.getSeatNumber() : 0, player != null ? player.getIdentity() : "null"});
        if (room == null) {
            log.error("[\u53d1\u9001\u8eab\u4efd] \u274c \u65e0\u6cd5\u53d1\u9001\u8eab\u4efd\uff1a\u623f\u95f4\u4e3a null");
            this.messagingTemplate.convertAndSendToUser(targetSessionId, "/queue/errors", (Object)Map.of((Object)"error", (Object)"\u65e0\u6cd5\u6062\u590d\u8eab\u4efd\u4fe1\u606f\uff1a\u623f\u95f4\u4e0d\u5b58\u5728"));
            return;
        }
        if (!room.isGameStarted()) {
            log.error("[\u53d1\u9001\u8eab\u4efd] \u274c \u65e0\u6cd5\u53d1\u9001\u8eab\u4efd\u7ed9\u73a9\u5bb6 {}: \u6e38\u620f\u672a\u5f00\u59cb", (Object)(player != null ? player.getPlayerId() : "\u672a\u77e5"));
            this.messagingTemplate.convertAndSendToUser(targetSessionId, "/queue/errors", (Object)Map.of((Object)"error", (Object)"\u65e0\u6cd5\u6062\u590d\u8eab\u4efd\u4fe1\u606f\uff1a\u6e38\u620f\u672a\u5f00\u59cb"));
            return;
        }
        if (player == null || player.getIdentity() == null) {
            log.error("[\u53d1\u9001\u8eab\u4efd] \u274c \u65e0\u6cd5\u53d1\u9001\u8eab\u4efd\u7ed9\u73a9\u5bb6: \u73a9\u5bb6\u6216\u8eab\u4efd\u4e3a null, player={}, identity={}", (Object)(player != null ? player.getPlayerId() : "null"), (Object)(player != null ? player.getIdentity() : "null"));
            this.messagingTemplate.convertAndSendToUser(targetSessionId, "/queue/errors", (Object)Map.of((Object)"error", (Object)"\u65e0\u6cd5\u6062\u590d\u8eab\u4efd\u4fe1\u606f\uff1a\u73a9\u5bb6\u8eab\u4efd\u4e3a\u7a7a"));
            return;
        }
        if (room.getShuffledIdentities() == null || room.getCurrentLeaderSeat() <= 0) {
            log.error("[\u53d1\u9001\u8eab\u4efd] \u274c \u65e0\u6cd5\u53d1\u9001\u8eab\u4efd\u7ed9\u73a9\u5bb6 {}: \u6e38\u620f\u4fe1\u606f\u4e0d\u5b8c\u6574, shuffledIdentities={}, currentLeaderSeat={}", new Object[]{player.getPlayerId(), room.getShuffledIdentities() != null ? room.getShuffledIdentities().size() : 0, room.getCurrentLeaderSeat()});
            this.messagingTemplate.convertAndSendToUser(targetSessionId, "/queue/errors", (Object)Map.of((Object)"error", (Object)"\u65e0\u6cd5\u6062\u590d\u8eab\u4efd\u4fe1\u606f\uff1a\u6e38\u620f\u4fe1\u606f\u4e22\u5931"));
            return;
        }
        log.info("[\u53d1\u9001\u8eab\u4efd] \u2705 \u5411\u91cd\u8fde\u73a9\u5bb6 {} \u53d1\u9001\u8eab\u4efd: {}, seatNumber={}, sessionId={}", new Object[]{player.getPlayerId(), player.getIdentity(), player.getSeatNumber(), targetSessionId});
        IdentityResponse payload = this.buildIdentityResponse(player.getSeatNumber(), player.getIdentity(), room.getShuffledIdentities(), room.getCurrentLeaderSeat());
        this.messagingTemplate.convertAndSendToUser(targetSessionId, "/queue/game-start", (Object)payload);
        log.info("[\u53d1\u9001\u8eab\u4efd] \u2705 \u8eab\u4efd\u4fe1\u606f\u5df2\u53d1\u9001: playerId={}, sessionId={}", (Object)player.getPlayerId(), (Object)targetSessionId);
    }

    private IdentityResponse buildIdentityResponse(int mySeatNumber, String myIdentity, List<String> shuffledIdentities, int leaderSeat) {
        RoleInfo myRoleInfo = ROLE_INFO_MAP.getOrDefault(myIdentity, UNKNOWN_ROLE);
        ArrayList<Integer> seenPlayers = new ArrayList<Integer>();
        String seenTitle = "\u4f60\u4ec0\u4e48\u4e5f\u6ca1\u770b\u5230";
        String seenSubtitle = "";
        boolean mordredInGame = shuffledIdentities.contains("\u83ab\u5fb7\u96f7\u5fb7");
        boolean oberonInGame = shuffledIdentities.contains("\u5965\u4f2f\u4f26");
        int redLancelotSeat = -1;
        int blueLancelotSeat = -1;
        int i = 0;
        while (i < shuffledIdentities.size()) {
            String role = shuffledIdentities.get(i);
            if (role.equals("\u5170\u65af\u6d1b\u7279\uff08\u7ea2\uff09")) {
                redLancelotSeat = i + 1;
            }
            if (role.equals("\u5170\u65af\u6d1b\u7279\uff08\u84dd\uff09")) {
                blueLancelotSeat = i + 1;
            }
            ++i;
        }
        Set merlinSees = Set.of((Object)"\u83ab\u7518\u5a1c", (Object)"\u523a\u5ba2", (Object)"\u5965\u4f2f\u4f26", (Object)"\u83ab\u5fb7\u96f7\u5fb7\u7684\u722a\u7259", (Object)"\u5170\u65af\u6d1b\u7279\uff08\u7ea2\uff09");
        Set allEvilRoles = Set.of((Object)"\u83ab\u7518\u5a1c", (Object)"\u523a\u5ba2", (Object)"\u83ab\u5fb7\u96f7\u5fb7\u7684\u722a\u7259", (Object)"\u83ab\u5fb7\u96f7\u5fb7", (Object)"\u5170\u65af\u6d1b\u7279\uff08\u7ea2\uff09");
        switch (myIdentity) {
            case "\u6885\u6797": {
                seenTitle = "\u4f60\u770b\u5230\u4e86 (\u574f\u4eba):";
                int i2 = 0;
                while (i2 < shuffledIdentities.size()) {
                    if (merlinSees.contains(shuffledIdentities.get(i2))) {
                        seenPlayers.add(i2 + 1);
                    }
                    ++i2;
                }
                seenSubtitle = mordredInGame ? "\u4f60\u65e0\u6cd5\u770b\u5230 \u83ab\u5fb7\u96f7\u5fb7\u3002" : "\uff08\u672c\u5c40\u6ca1\u6709\u83ab\u5fb7\u96f7\u5fb7\uff09";
                break;
            }
            case "\u6d3e\u897f\u7ef4\u5c14": {
                seenTitle = "\u4f60\u770b\u5230\u4e86 (\u6885\u6797/\u83ab\u7518\u5a1c):";
                seenSubtitle = "\u4ed6\u4eec\u4e4b\u4e2d\u4e00\u4e2a\u662f\u6885\u6797\uff0c\u4e00\u4e2a\u662f\u83ab\u7518\u5a1c\u3002";
                int i3 = 0;
                while (i3 < shuffledIdentities.size()) {
                    if (Set.of((Object)"\u6885\u6797", (Object)"\u83ab\u7518\u5a1c").contains(shuffledIdentities.get(i3))) {
                        seenPlayers.add(i3 + 1);
                    }
                    ++i3;
                }
                break;
            }
            case "\u83ab\u5fb7\u96f7\u5fb7\u7684\u722a\u7259": 
            case "\u523a\u5ba2": 
            case "\u83ab\u7518\u5a1c": 
            case "\u83ab\u5fb7\u96f7\u5fb7": {
                seenTitle = "\u4f60\u770b\u5230\u4e86 (\u4f60\u7684\u961f\u53cb):";
                int i4 = 0;
                while (i4 < shuffledIdentities.size()) {
                    String role = shuffledIdentities.get(i4);
                    int seat = i4 + 1;
                    if (allEvilRoles.contains(role) && seat != mySeatNumber) {
                        seenPlayers.add(seat);
                    }
                    ++i4;
                }
                seenSubtitle = oberonInGame ? "\u4f60\u65e0\u6cd5\u770b\u5230 \u5965\u4f2f\u4f26\u3002" : "\uff08\u672c\u5c40\u6ca1\u6709\u5965\u4f2f\u4f26\uff09";
                break;
            }
            case "\u5170\u65af\u6d1b\u7279\uff08\u84dd\uff09": {
                if (redLancelotSeat > 0) {
                    seenTitle = "\u4f60\u770b\u5230\u4e86 (\u7ea2\u5170\u65af\u6d1b\u7279):";
                    seenSubtitle = "\u4ed6\u662f\u4f60\u7684\u5bbf\u654c\u3002";
                    seenPlayers.add(redLancelotSeat);
                    break;
                }
                seenTitle = "\u4f60\u4ec0\u4e48\u4e5f\u6ca1\u770b\u5230";
                seenSubtitle = "\u672c\u5c40\u6ca1\u6709\u7ea2\u5170\u65af\u6d1b\u7279\u3002";
                break;
            }
            case "\u5170\u65af\u6d1b\u7279\uff08\u7ea2\uff09": {
                if (blueLancelotSeat > 0) {
                    seenTitle = "\u4f60\u770b\u5230\u4e86 (\u84dd\u5170\u65af\u6d1b\u7279):";
                    seenSubtitle = "\u4ed6\u662f\u4f60\u7684\u5bf9\u624b\u3002";
                    seenPlayers.add(blueLancelotSeat);
                    break;
                }
                seenTitle = "\u4f60\u4ec0\u4e48\u4e5f\u6ca1\u770b\u5230";
                seenSubtitle = "\u672c\u5c40\u6ca1\u6709\u84dd\u5170\u65af\u6d1b\u7279\u3002";
                break;
            }
            default: {
                seenSubtitle = "\u4e0d\u662f\u5148\u77e5\uff0c\u80dc\u4f3c\u5148\u77e5";
            }
        }
        if (seenPlayers.isEmpty() && seenTitle.equals("\u4f60\u4ec0\u4e48\u4e5f\u6ca1\u770b\u5230") && seenSubtitle.isEmpty()) {
            seenSubtitle = "\u4e0d\u662f\u5148\u77e5\uff0c\u80dc\u4f3c\u5148\u77e5";
        }
        return IdentityResponse.builder().identity(myIdentity).emoji(myRoleInfo.emoji()).camp(myRoleInfo.camp()).seenTitle(seenTitle).seenPlayers(seenPlayers).seenSubtitle(seenSubtitle).imageUrl(myRoleInfo.imageUrl()).leaderSeat(leaderSeat).build();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void resetGame(GameRoom room, String requestingPlayerId) {
        if (room == null) {
            log.warn("\u5c1d\u8bd5\u91cd\u7f6e null \u623f\u95f4");
            return;
        }
        String roomNumber = room.getRoomNumber();
        GameRoom gameRoom = room;
        synchronized (gameRoom) {
            if (room.isGameStarted()) {
                log.info("\u623f\u95f4 {} \u6e38\u620f\u72b6\u6001\u91cd\u7f6e (\u73a9\u5bb6 {})", (Object)roomNumber, (Object)requestingPlayerId);
                room.setGameStarted(false);
                room.getInitialPlayers().clear();
                room.getPlayers().values().forEach(player -> {
                    player.setReady(false);
                    player.setIdentity(null);
                });
                room.setShuffledIdentities(null);
                room.setCurrentLeaderSeat(0);
                room.setCurrentLeaderIndex(0);
                room.setCurrentMissionRound(0);
                room.setSelectedMissionMembers(new HashSet());
                room.setMissionProposerSessionId(null);
                room.setVotes(new ConcurrentHashMap());
                room.setMissionVotingCompleted(false);
                room.setMissionResult(null);
                room.setMissionFailCount(0);
                room.setMissionHistory(new ArrayList());
                room.setLakeLadyHolderSeat(null);
                room.setLakeLadyUsedSeats(new HashSet());
                room.setLakeLadyInspectionHistory(new HashMap());
                room.setLakeLadyInspectionResult(new HashMap());
                room.setLakeLadyPublicResult(new HashMap());
                room.setLakeLadyLastInspectorSeat(null);
            } else {
                log.debug("\u623f\u95f4 {} \u65e0\u9700\u91cd\u590d\u91cd\u7f6e", (Object)roomNumber);
            }
        }
    }

    private void handleInvalidConfig(GameRoom room, String errorMessage) {
        log.error("\u623f\u95f4 {} \u914d\u7f6e\u9519\u8bef: {}", (Object)room.getRoomNumber(), (Object)errorMessage);
        room.setGameStarted(false);
        room.getPlayers().keySet().forEach(sid -> this.messagingTemplate.convertAndSendToUser(sid, "/queue/errors", (Object)Map.of((Object)"error", (Object)errorMessage)));
    }

    @Generated
    public AwlFjGameService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
}

