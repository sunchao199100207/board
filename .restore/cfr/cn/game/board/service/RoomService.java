/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cn.game.board.model.avalon.dto.GameRoom
 *  cn.game.board.model.avalon.dto.Player
 *  cn.game.board.model.avalon.dto.SpectatorPlayerView
 *  cn.game.board.model.avalon.dto.SpectatorRoomView
 *  cn.game.board.service.AwlFjGameService
 *  cn.game.board.service.RoomService
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.data.redis.core.RedisTemplate
 *  org.springframework.data.redis.core.StringRedisTemplate
 *  org.springframework.messaging.simp.SimpMessagingTemplate
 *  org.springframework.scheduling.TaskScheduler
 *  org.springframework.stereotype.Service
 */
package cn.game.board.service;

import cn.game.board.model.avalon.dto.GameRoom;
import cn.game.board.model.avalon.dto.Player;
import cn.game.board.model.avalon.dto.SpectatorPlayerView;
import cn.game.board.model.avalon.dto.SpectatorRoomView;
import cn.game.board.service.AwlFjGameService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

@Service
public class RoomService {
    private static final Logger log = LoggerFactory.getLogger(RoomService.class);
    public static final String ROOM_KEY_PREFIX = "avalon:room:";
    public static final String PLAYER_SEAT_KEY_PREFIX = "avalon:player:seat:";
    private static final long ROOM_EXPIRATION_HOURS = 2L;
    private static final String PREFERRED_ROLE_CLEAR_TOKEN = "__CLEAR__";
    private final RedisTemplate<String, GameRoom> gameRoomRedisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final AwlFjGameService awlFjGameService;
    private final TaskScheduler taskScheduler;
    private final Map<String, ScheduledFuture<?>> pendingSeatSelections = new ConcurrentHashMap();
    private final StringRedisTemplate stringRedisTemplate;
    @Value(value="${app.game.afk.seat-select-timeout-ms:30000}")
    private long seatSelectTimeoutMs;
    private static final Set<String> ALLOWED_FORCED_ROLES = Set.of((Object)"\u6885\u6797", (Object)"\u6d3e\u897f\u7ef4\u5c14", (Object)"\u4e9a\u745f\u7684\u5fe0\u81e3");

    public Set<String> getActiveRoomKeys() {
        return this.gameRoomRedisTemplate.keys((Object)"avalon:room:*");
    }

    public GameRoom getRoomByKey(String roomKey) {
        return (GameRoom)this.gameRoomRedisTemplate.opsForValue().get((Object)roomKey);
    }

    public SpectatorRoomView buildSpectatorView(String roomNumber) {
        Map sourcePlayers;
        GameRoom room = this.findRoomOrFail(roomNumber);
        SpectatorRoomView view = new SpectatorRoomView();
        view.setRoomNumber(roomNumber);
        view.setGameStarted(room.isGameStarted());
        view.setTotalPlayers(room.getTotalPlayers());
        view.setCurrentLeaderSeat(room.getCurrentLeaderSeat());
        view.setCurrentMissionRound(room.getCurrentMissionRound());
        if (room.isGameStarted() && room.getInitialPlayers() != null && !room.getInitialPlayers().isEmpty()) {
            sourcePlayers = room.getInitialPlayers();
            log.debug("[\u89c2\u6218] \u623f\u95f4 {} \u6e38\u620f\u5df2\u5f00\u59cb\uff0c\u4f7f\u7528 initialPlayers\uff0c\u5171 {} \u4eba", (Object)roomNumber, (Object)sourcePlayers.size());
        } else {
            sourcePlayers = room.getPlayers();
            log.debug("[\u89c2\u6218] \u623f\u95f4 {} \u6e38\u620f\u672a\u5f00\u59cb\uff0c\u4f7f\u7528\u5728\u7ebf\u73a9\u5bb6\u5217\u8868\uff0c\u5171 {} \u4eba", (Object)roomNumber, (Object)(sourcePlayers != null ? sourcePlayers.size() : 0));
        }
        if (sourcePlayers == null || sourcePlayers.isEmpty()) {
            sourcePlayers = room.getPlayers();
            log.debug("[\u89c2\u6218] \u623f\u95f4 {} \u4f7f\u7528\u540e\u5907\u73a9\u5bb6\u5217\u8868\uff0c\u5171 {} \u4eba", (Object)roomNumber, (Object)(sourcePlayers != null ? sourcePlayers.size() : 0));
        }
        List<Object> playerViews = new ArrayList();
        if (sourcePlayers != null && !sourcePlayers.isEmpty()) {
            Set evilIdentities = Set.of((Object)"\u83ab\u7518\u5a1c", (Object)"\u523a\u5ba2", (Object)"\u83ab\u5fb7\u96f7\u5fb7\u7684\u722a\u7259", (Object)"\u83ab\u5fb7\u96f7\u5fb7", (Object)"\u5965\u4f2f\u4f26", (Object)"\u5170\u65af\u6d1b\u7279\uff08\u7ea2\uff09");
            playerViews = sourcePlayers.values().stream().filter(p -> p.getSeatNumber() > 0).sorted(Comparator.comparingInt(Player::getSeatNumber)).map(p -> {
                String identity = p.getIdentity();
                String camp = identity != null && evilIdentities.contains(identity) ? "evil" : "good";
                return new SpectatorPlayerView(p.getSeatNumber(), p.getPlayerId(), identity, camp);
            }).collect(Collectors.toList());
        }
        view.setPlayers(playerViews);
        log.info("[\u89c2\u6218] \u623f\u95f4 {} \u89c2\u6218\u89c6\u56fe\u6784\u5efa\u5b8c\u6210\uff1a\u6e38\u620f\u72b6\u6001={}, \u603b\u4eba\u6570={}, \u73a9\u5bb6\u6570={}", new Object[]{roomNumber, room.isGameStarted(), room.getTotalPlayers(), playerViews.size()});
        return view;
    }

    public GameRoom createRoom(String roomNumber, int totalPlayers, String hostSessionId, String preferredRole) {
        if (totalPlayers < 5 || totalPlayers > 12) {
            throw new RuntimeException("\u4e0d\u652f\u6301\u7684\u4eba\u6570: " + totalPlayers + " (\u4ec5\u652f\u6301 5-12 \u4eba)");
        }
        String roomKey = ROOM_KEY_PREFIX + roomNumber;
        GameRoom existingRoom = (GameRoom)this.gameRoomRedisTemplate.opsForValue().get((Object)roomKey);
        if (existingRoom != null) {
            throw new RuntimeException("\u623f\u95f4 " + roomNumber + " \u5df2\u5b58\u5728");
        }
        log.info("\u521b\u5efa\u623f\u95f4 {}", (Object)roomNumber);
        GameRoom room = new GameRoom(roomNumber, totalPlayers, null);
        Player hostPlayer = new Player(hostSessionId);
        this.assignPreferredRole(hostPlayer, preferredRole, "\u521b\u5efa\u623f\u95f4");
        room.getPlayers().put(hostSessionId, hostPlayer);
        room.setHostPlayerId(hostPlayer.getPlayerId());
        this.saveRoom(room);
        this.putSessionRoomMapping(hostSessionId, roomNumber);
        this.putPlayerSessionMapping(hostPlayer.getPlayerId(), hostSessionId);
        this.scheduleSeatSelectionKick(roomNumber, hostPlayer.getPlayerId(), hostSessionId);
        this.broadcastRoomState(roomNumber);
        return room;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public GameRoom joinRoom(String roomNumber, String playerSessionId, String preferredRole) {
        GameRoom room = this.findRoomOrFail(roomNumber);
        if (room.getTotalPlayers() < 5 || room.getTotalPlayers() > 12) {
            throw new RuntimeException("\u623f\u95f4\u914d\u7f6e\u9519\u8bef");
        }
        GameRoom gameRoom = room;
        synchronized (gameRoom) {
            if (room.getPlayers().containsKey(playerSessionId)) {
                log.warn("\u73a9\u5bb6 {} \u5df2\u5728\u623f\u95f4 {} \u4e2d", (Object)playerSessionId, (Object)roomNumber);
                return room;
            }
            if (room.getPlayers().size() >= room.getTotalPlayers()) {
                throw new RuntimeException("\u623f\u95f4 " + roomNumber + " \u5df2\u6ee1");
            }
            Player newPlayer = new Player(playerSessionId);
            this.assignPreferredRole(newPlayer, preferredRole, "\u52a0\u5165\u623f\u95f4");
            room.getPlayers().put(playerSessionId, newPlayer);
            log.info("\u73a9\u5bb6 {} \u52a0\u5165\u623f\u95f4 {}", (Object)playerSessionId, (Object)roomNumber);
            this.saveRoom(room);
            this.putSessionRoomMapping(playerSessionId, roomNumber);
            this.putPlayerSessionMapping(newPlayer.getPlayerId(), playerSessionId);
            this.scheduleSeatSelectionKick(roomNumber, newPlayer.getPlayerId(), playerSessionId);
        }
        this.broadcastRoomState(roomNumber);
        return room;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void rejoinWithId(String roomNumber, String playerId, String newSessionId, String preferredRole) {
        GameRoom room;
        log.info("[\u91cd\u8fde] \u5f00\u59cb\u5904\u7406\u91cd\u8fde: roomNumber={}, playerId={}, newSessionId={}", new Object[]{roomNumber, playerId, newSessionId});
        GameRoom gameRoom = room = this.findRoomOrFail(roomNumber);
        synchronized (gameRoom) {
            Optional playerFromSnapshot;
            Player playerToRejoin = null;
            String oldSessionId = null;
            log.info("[\u91cd\u8fde] \u623f\u95f4\u72b6\u6001: gameStarted={}, playersSize={}, initialPlayersSize={}", new Object[]{room.isGameStarted(), room.getPlayers() != null ? room.getPlayers().size() : 0, room.getInitialPlayers() != null ? room.getInitialPlayers().size() : 0});
            Optional<Object> optional = playerFromSnapshot = room.getInitialPlayers() != null && !room.getInitialPlayers().isEmpty() ? room.getInitialPlayers().values().stream().filter(p -> p.getPlayerId().equals(playerId)).findFirst() : Optional.empty();
            if (playerFromSnapshot.isPresent()) {
                Optional<Map.Entry> onlineSamePlayer;
                playerToRejoin = (Player)playerFromSnapshot.get();
                oldSessionId = playerToRejoin.getSessionId();
                log.info("[\u91cd\u8fde] \u2705 \u4ece initialPlayers \u627e\u5230\u73a9\u5bb6: playerId={}, seatNumber={}, identity={}, oldSessionId={}, newSessionId={}", new Object[]{playerId, playerToRejoin.getSeatNumber(), playerToRejoin.getIdentity(), oldSessionId, newSessionId});
                boolean alreadyReconnected = room.getPlayers().values().stream().anyMatch(p -> p.getPlayerId().equals(playerId));
                if (alreadyReconnected && (onlineSamePlayer = room.getPlayers().entrySet().stream().filter(e -> ((Player)e.getValue()).getPlayerId().equals(playerId)).findFirst()).isPresent()) {
                    String existingSession = (String)onlineSamePlayer.get().getKey();
                    room.getPlayers().remove(existingSession);
                    this.deleteSessionRoomMapping(existingSession);
                    log.warn("[\u91cd\u8fde] \u73a9\u5bb6 {} \u5df2\u5728\u7ebf\uff0c\u66ff\u6362\u4f1a\u8bdd {} -> {}", new Object[]{playerId, existingSession, newSessionId});
                }
                playerToRejoin.setSessionId(newSessionId);
                playerToRejoin.setReady(false);
                room.getPlayers().put(newSessionId, playerToRejoin);
                this.applyPreferredRoleOverride(playerToRejoin, preferredRole);
                this.cancelSeatSelectionKick(playerToRejoin.getPlayerId());
                if (playerToRejoin.getSeatNumber() == 0) {
                    this.scheduleSeatSelectionKick(roomNumber, playerToRejoin.getPlayerId(), newSessionId);
                }
                this.putSessionRoomMapping(newSessionId, roomNumber);
                this.putPlayerSessionMapping(playerToRejoin.getPlayerId(), newSessionId);
                this.saveRoom(room);
                if (room.isGameStarted()) {
                    log.info("[\u91cd\u8fde] \u6e38\u620f\u5df2\u5f00\u59cb\uff0c\u5411\u91cd\u8fde\u73a9\u5bb6 {} \u53d1\u9001\u8eab\u4efd\u4fe1\u606f", (Object)playerId);
                    this.awlFjGameService.sendIdentityToPlayer(room, playerToRejoin, newSessionId);
                }
            } else {
                log.info("[\u91cd\u8fde] initialPlayers \u4e2d\u672a\u627e\u5230\u73a9\u5bb6\uff0c\u68c0\u67e5\u5728\u7ebf\u5217\u8868: playerId={}", (Object)playerId);
                Optional<Map.Entry> playerEntry = room.getPlayers().entrySet().stream().filter(entry -> ((Player)entry.getValue()).getPlayerId().equals(playerId)).findFirst();
                if (playerEntry.isPresent()) {
                    log.info("[\u91cd\u8fde] \u2705 \u5728\u5728\u7ebf\u5217\u8868\u4e2d\u627e\u5230\u73a9\u5bb6\uff0c\u66f4\u65b0 Session: playerId={}", (Object)playerId);
                    playerToRejoin = (Player)playerEntry.get().getValue();
                    oldSessionId = (String)playerEntry.get().getKey();
                    if (!oldSessionId.equals(newSessionId)) {
                        room.getPlayers().remove(oldSessionId);
                        playerToRejoin.setSessionId(newSessionId);
                        room.getPlayers().put(newSessionId, playerToRejoin);
                    }
                    playerToRejoin.setReady(false);
                    this.cancelSeatSelectionKick(playerId);
                    if (playerToRejoin.getSeatNumber() == 0) {
                        this.scheduleSeatSelectionKick(roomNumber, playerId, newSessionId);
                    }
                    this.deleteSessionRoomMapping(oldSessionId);
                    this.putSessionRoomMapping(newSessionId, roomNumber);
                    this.putPlayerSessionMapping(playerId, newSessionId);
                    this.saveRoom(room);
                    this.applyPreferredRoleOverride(playerToRejoin, preferredRole);
                    if (room.isGameStarted() && playerToRejoin.getIdentity() != null) {
                        log.info("[\u91cd\u8fde] \u6e38\u620f\u5df2\u5f00\u59cb\uff0c\u5411\u91cd\u8fde\u73a9\u5bb6 {} \u53d1\u9001\u8eab\u4efd\u4fe1\u606f", (Object)playerId);
                        this.awlFjGameService.sendIdentityToPlayer(room, playerToRejoin, newSessionId);
                    }
                } else {
                    Optional initialPlayer;
                    log.warn("[\u91cd\u8fde] \u26a0\ufe0f \u5728\u7ebf\u5217\u8868\u4e2d\u672a\u627e\u5230\u73a9\u5bb6\uff0c\u68c0\u67e5 initialPlayers: playerId={}", (Object)playerId);
                    Optional<Object> optional2 = initialPlayer = room.getInitialPlayers() != null && !room.getInitialPlayers().isEmpty() ? room.getInitialPlayers().values().stream().filter(p -> p.getPlayerId().equals(playerId)).findFirst() : Optional.empty();
                    if (initialPlayer.isPresent()) {
                        log.info("[\u91cd\u8fde] \u2705 \u5728 initialPlayers \u4e2d\u627e\u5230\u73a9\u5bb6\uff08\u7b2c\u4e8c\u6b21\u68c0\u67e5\uff09\uff0c\u6062\u590d\u73a9\u5bb6\u5230\u5728\u7ebf\u5217\u8868: playerId={}", (Object)playerId);
                        playerToRejoin = (Player)initialPlayer.get();
                        playerToRejoin.setSessionId(newSessionId);
                        playerToRejoin.setReady(false);
                        room.getPlayers().put(newSessionId, playerToRejoin);
                        this.applyPreferredRoleOverride(playerToRejoin, preferredRole);
                        this.cancelSeatSelectionKick(playerId);
                        if (playerToRejoin.getSeatNumber() == 0) {
                            this.scheduleSeatSelectionKick(roomNumber, playerToRejoin.getPlayerId(), newSessionId);
                        }
                        this.putSessionRoomMapping(newSessionId, roomNumber);
                        this.putPlayerSessionMapping(playerToRejoin.getPlayerId(), newSessionId);
                        this.saveRoom(room);
                        if (room.isGameStarted()) {
                            log.info("[\u91cd\u8fde] \u6e38\u620f\u5df2\u5f00\u59cb\uff0c\u5411\u91cd\u8fde\u73a9\u5bb6 {} \u53d1\u9001\u8eab\u4efd\u4fe1\u606f", (Object)playerId);
                            this.awlFjGameService.sendIdentityToPlayer(room, playerToRejoin, newSessionId);
                        }
                    } else if (!room.isGameStarted()) {
                        log.info("[\u91cd\u8fde] \u6e38\u620f\u672a\u5f00\u59cb\uff0c\u5141\u8bb8\u91cd\u65b0\u52a0\u5165: playerId={}, roomNumber={}", (Object)playerId, (Object)roomNumber);
                        if (room.getPlayers().size() >= room.getTotalPlayers()) {
                            log.error("[\u91cd\u8fde] \u274c \u623f\u95f4 {} \u5df2\u6ee1\uff0c\u65e0\u6cd5\u91cd\u8fde: currentSize={}, totalPlayers={}", new Object[]{roomNumber, room.getPlayers().size(), room.getTotalPlayers()});
                            throw new RuntimeException("\u623f\u95f4 " + roomNumber + " \u5df2\u6ee1\uff0c\u65e0\u6cd5\u91cd\u8fde");
                        }
                        Player newPlayer = new Player(newSessionId);
                        newPlayer.setPlayerId(playerId);
                        String seatKey = PLAYER_SEAT_KEY_PREFIX + roomNumber + ":" + playerId;
                        String savedSeatNumber = (String)this.stringRedisTemplate.opsForValue().get((Object)seatKey);
                        if (savedSeatNumber != null) {
                            try {
                                int seatNum = Integer.parseInt(savedSeatNumber);
                                boolean seatTaken = room.getPlayers().values().stream().anyMatch(p -> p.getSeatNumber() == seatNum);
                                if (!seatTaken && seatNum > 0 && seatNum <= room.getTotalPlayers()) {
                                    newPlayer.setSeatNumber(seatNum);
                                    log.info("[\u91cd\u8fde] \u2705 \u4ece Redis \u6062\u590d\u73a9\u5bb6\u5ea7\u4f4d\u53f7: playerId={}, seatNumber={}", (Object)playerId, (Object)seatNum);
                                    this.stringRedisTemplate.delete((Object)seatKey);
                                } else {
                                    log.warn("[\u91cd\u8fde] \u26a0\ufe0f \u4fdd\u5b58\u7684\u5ea7\u4f4d\u53f7 {} \u5df2\u88ab\u5360\u7528\u6216\u65e0\u6548\uff0c\u4e0d\u6062\u590d: playerId={}", (Object)seatNum, (Object)playerId);
                                }
                            }
                            catch (NumberFormatException e2) {
                                log.warn("[\u91cd\u8fde] \u26a0\ufe0f \u65e0\u6cd5\u89e3\u6790\u4fdd\u5b58\u7684\u5ea7\u4f4d\u53f7: {}", (Object)savedSeatNumber);
                            }
                        }
                        room.getPlayers().put(newSessionId, newPlayer);
                        this.applyPreferredRoleOverride(newPlayer, preferredRole);
                        this.putSessionRoomMapping(newSessionId, roomNumber);
                        this.putPlayerSessionMapping(playerId, newSessionId);
                        if (newPlayer.getSeatNumber() == 0) {
                            this.scheduleSeatSelectionKick(roomNumber, playerId, newSessionId);
                        }
                        this.saveRoom(room);
                        log.info("[\u91cd\u8fde] \u2705 \u73a9\u5bb6 {} \u91cd\u65b0\u52a0\u5165\u623f\u95f4 {} \u6210\u529f\uff08\u6e38\u620f\u672a\u5f00\u59cb\uff09\uff0c\u5ea7\u4f4d\u53f7={}", new Object[]{playerId, roomNumber, newPlayer.getSeatNumber()});
                    } else {
                        log.error("[\u91cd\u8fde] \u274c \u91cd\u8fde\u5931\u8d25\uff1a\u672a\u627e\u5230\u73a9\u5bb6 {}\uff0c\u65e2\u4e0d\u5728\u5728\u7ebf\u5217\u8868\u4e5f\u4e0d\u5728\u521d\u59cb\u73a9\u5bb6\u5217\u8868\uff0c\u4e14\u6e38\u620f\u5df2\u5f00\u59cb", (Object)playerId);
                        log.error("[\u91cd\u8fde] \u623f\u95f4\u8be6\u60c5: roomNumber={}, gameStarted={}, playersSize={}, initialPlayersSize={}", new Object[]{roomNumber, room.isGameStarted(), room.getPlayers() != null ? room.getPlayers().size() : 0, room.getInitialPlayers() != null ? room.getInitialPlayers().size() : 0});
                        if (room.getPlayers() != null && !room.getPlayers().isEmpty()) {
                            log.error("[\u91cd\u8fde] \u5728\u7ebf\u73a9\u5bb6ID\u5217\u8868: {}", room.getPlayers().values().stream().map(Player::getPlayerId).collect(Collectors.toList()));
                        }
                        if (room.getInitialPlayers() != null && !room.getInitialPlayers().isEmpty()) {
                            log.error("[\u91cd\u8fde] \u521d\u59cb\u73a9\u5bb6ID\u5217\u8868: {}", room.getInitialPlayers().values().stream().map(Player::getPlayerId).collect(Collectors.toList()));
                        }
                        throw new RuntimeException("\u60a8\u5df2\u4e0d\u5728\u623f\u95f4 " + roomNumber + " \u4e2d");
                    }
                }
            }
        }
        log.info("[\u91cd\u8fde] \u2705 \u91cd\u8fde\u5904\u7406\u5b8c\u6210\uff0c\u5e7f\u64ad\u623f\u95f4\u72b6\u6001: roomNumber={}, playerId={}", (Object)roomNumber, (Object)playerId);
        this.broadcastRoomState(roomNumber);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void selectSeat(String roomNumber, String playerSessionId, int seatNumber) {
        GameRoom room;
        GameRoom gameRoom = room = this.findRoomOrFail(roomNumber);
        synchronized (gameRoom) {
            if (seatNumber < 1 || seatNumber > room.getTotalPlayers()) {
                throw new RuntimeException("\u65e0\u6548\u7684\u5ea7\u4f4d\u53f7: " + seatNumber);
            }
            for (Player p : room.getPlayers().values()) {
                if (p.getSeatNumber() != seatNumber || p.getSessionId().equals(playerSessionId)) continue;
                throw new RuntimeException("\u5ea7\u4f4d " + seatNumber + " \u5df2\u88ab\u5360\u7528");
            }
            Player player = (Player)room.getPlayers().get(playerSessionId);
            if (player != null) {
                if (player.getSeatNumber() != seatNumber) {
                    log.info("\u73a9\u5bb6 {} \u9009\u62e9\u5ea7\u4f4d {}", (Object)playerSessionId, (Object)seatNumber);
                    this.cancelSeatSelectionKick(player.getPlayerId());
                    player.setSeatNumber(seatNumber);
                    player.setReady(false);
                    this.saveRoom(room);
                    this.broadcastRoomState(roomNumber);
                }
            } else {
                log.warn("\u73a9\u5bb6 {} \u4e0d\u5728\u623f\u95f4 {}", (Object)playerSessionId, (Object)roomNumber);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void updatePreferredRole(String roomNumber, String playerSessionId, String playerId, String preferredRole) {
        GameRoom room;
        GameRoom gameRoom = room = this.findRoomOrFail(roomNumber);
        synchronized (gameRoom) {
            Optional<Player> initialPlayer;
            Optional<Map.Entry> entry;
            Player player = (Player)room.getPlayers().get(playerSessionId);
            if (player == null && playerId != null && (entry = room.getPlayers().entrySet().stream().filter(e -> playerId.equals(((Player)e.getValue()).getPlayerId())).findFirst()).isPresent()) {
                String oldSessionId = (String)entry.get().getKey();
                player = (Player)entry.get().getValue();
                if (!oldSessionId.equals(playerSessionId)) {
                    room.getPlayers().remove(oldSessionId);
                    player.setSessionId(playerSessionId);
                    room.getPlayers().put(playerSessionId, player);
                    this.deleteSessionRoomMapping(oldSessionId);
                    this.putSessionRoomMapping(playerSessionId, roomNumber);
                    this.putPlayerSessionMapping(player.getPlayerId(), playerSessionId);
                    log.info("[\u66f4\u65b0\u9996\u9009\u8eab\u4efd] \u73a9\u5bb6 {} sessionId \u7531 {} \u66ff\u6362\u4e3a {}", new Object[]{player.getPlayerId(), oldSessionId, playerSessionId});
                }
            }
            if (player == null && playerId != null && room.getInitialPlayers() != null && !room.getInitialPlayers().isEmpty() && (initialPlayer = room.getInitialPlayers().values().stream().filter(p -> playerId.equals(p.getPlayerId())).findFirst()).isPresent()) {
                player = initialPlayer.get();
                log.info("[\u66f4\u65b0\u9996\u9009\u8eab\u4efd] \u73a9\u5bb6 {} \u5f53\u524d\u4e0d\u5728\u7ebf\uff0c\u66f4\u65b0 initialPlayers \u8bb0\u5f55", (Object)player.getPlayerId());
            }
            if (player == null) {
                throw new RuntimeException("\u73a9\u5bb6\u4e0d\u5728\u623f\u95f4\u4e2d\uff0c\u65e0\u6cd5\u66f4\u65b0\u9996\u9009\u8eab\u4efd");
            }
            this.assignPreferredRole(player, preferredRole, "\u66f4\u65b0\u9996\u9009\u8eab\u4efd");
            this.saveRoom(room);
            log.info("[\u66f4\u65b0\u9996\u9009\u8eab\u4efd] \u73a9\u5bb6 {} \u7684\u9996\u9009\u8eab\u4efd\u5df2\u66f4\u65b0\u4e3a {}", (Object)player.getPlayerId(), (Object)player.getPreferredRole());
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void setReady(String roomNumber, String playerSessionId, boolean isReady) {
        GameRoom room;
        GameRoom gameRoom = room = this.findRoomOrFail(roomNumber);
        synchronized (gameRoom) {
            Player player = (Player)room.getPlayers().get(playerSessionId);
            if (player != null && player.getSeatNumber() != 0) {
                if (player.isReady() != isReady) {
                    log.info("\u73a9\u5bb6 {} \u8bbe\u7f6e\u51c6\u5907\u72b6\u6001: {}", (Object)playerSessionId, (Object)isReady);
                    player.setReady(isReady);
                    this.awlFjGameService.checkGameStart(room);
                    this.saveRoom(room);
                    this.broadcastRoomState(roomNumber);
                }
            } else if (player == null) {
                log.warn("\u73a9\u5bb6 {} \u4e0d\u5728\u623f\u95f4 {}", (Object)playerSessionId, (Object)roomNumber);
            } else {
                log.warn("\u73a9\u5bb6 {} \u5c1a\u672a\u9009\u5ea7", (Object)playerSessionId);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void removePlayer(String roomNumber, String playerSessionId) {
        GameRoom room;
        log.info("[\u79fb\u9664\u73a9\u5bb6] \u5f00\u59cb\u79fb\u9664\u73a9\u5bb6: roomNumber={}, sessionId={}", (Object)roomNumber, (Object)playerSessionId);
        GameRoom gameRoom = room = this.findRoomOrFail(roomNumber);
        synchronized (gameRoom) {
            Player removedPlayer = (Player)room.getPlayers().remove(playerSessionId);
            if (removedPlayer == null) {
                log.debug("[\u79fb\u9664\u73a9\u5bb6] \u73a9\u5bb6 {} \u5df2\u4e0d\u5728\u5728\u7ebf\u5217\u8868\u4e2d", (Object)playerSessionId);
                return;
            }
            this.cancelSeatSelectionKick(removedPlayer.getPlayerId());
            log.info("[\u79fb\u9664\u73a9\u5bb6] \u73a9\u5bb6 {} \u5df2\u4ece\u5728\u7ebf\u5217\u8868\u79fb\u9664 (Session: {}, Seat: {}, Identity: {}, GameStarted: {})", new Object[]{removedPlayer.getPlayerId(), playerSessionId, removedPlayer.getSeatNumber(), removedPlayer.getIdentity(), room.isGameStarted()});
            if (!room.isGameStarted() && removedPlayer.getSeatNumber() > 0) {
                String seatKey = PLAYER_SEAT_KEY_PREFIX + roomNumber + ":" + removedPlayer.getPlayerId();
                this.stringRedisTemplate.opsForValue().set((Object)seatKey, (Object)String.valueOf(removedPlayer.getSeatNumber()), 2L, TimeUnit.HOURS);
                log.info("[\u79fb\u9664\u73a9\u5bb6] \u4fdd\u5b58\u73a9\u5bb6\u5ea7\u4f4d\u53f7\u4fe1\u606f\u5230 Redis: playerId={}, seatNumber={}, key={}", new Object[]{removedPlayer.getPlayerId(), removedPlayer.getSeatNumber(), seatKey});
            }
            log.info("[\u79fb\u9664\u73a9\u5bb6] \u4fdd\u7559 initialPlayers \u4e2d\u7684\u73a9\u5bb6\u4fe1\u606f\u4ee5\u4fbf\u91cd\u8fde: playerId={}, initialPlayersSize={}", (Object)removedPlayer.getPlayerId(), (Object)(room.getInitialPlayers() != null ? room.getInitialPlayers().size() : 0));
            this.deleteSessionRoomMapping(playerSessionId);
            String mapped = this.getPlayerSessionMapping(removedPlayer.getPlayerId());
            if (playerSessionId.equals(mapped)) {
                this.deletePlayerSessionMapping(removedPlayer.getPlayerId());
                log.debug("[\u79fb\u9664\u73a9\u5bb6] \u5df2\u5220\u9664\u73a9\u5bb6 {} \u7684 session \u6620\u5c04", (Object)removedPlayer.getPlayerId());
            }
            boolean roomDisbanded = false;
            if (room.getPlayers().isEmpty() && !room.isGameStarted()) {
                this.deleteRoom(roomNumber);
                log.info("[\u79fb\u9664\u73a9\u5bb6] \u623f\u95f4 {} \u5df2\u89e3\u6563\uff08\u6240\u6709\u73a9\u5bb6\u79bb\u5f00\u4e14\u6e38\u620f\u672a\u5f00\u59cb\uff09", (Object)roomNumber);
                roomDisbanded = true;
            } else if (room.getPlayers().isEmpty() && room.isGameStarted()) {
                log.info("[\u79fb\u9664\u73a9\u5bb6] \u623f\u95f4 {} \u6240\u6709\u73a9\u5bb6\u5747\u5df2\u65ad\u7ebf\uff0c\u7b49\u5f85\u91cd\u8fde\uff08\u6e38\u620f\u5df2\u5f00\u59cb\uff0c\u4fdd\u7559\u623f\u95f4\uff09", (Object)roomNumber);
            }
            if (!roomDisbanded) {
                if (removedPlayer.getPlayerId().equals(room.getHostPlayerId())) {
                    Optional newHost = room.getPlayers().values().stream().findFirst();
                    if (newHost.isPresent()) {
                        room.setHostPlayerId(((Player)newHost.get()).getPlayerId());
                        log.info("[\u79fb\u9664\u73a9\u5bb6] \u623f\u4e3b {} \u65ad\u7ebf\uff0c\u65b0\u623f\u4e3b\u662f {}", (Object)removedPlayer.getPlayerId(), (Object)((Player)newHost.get()).getPlayerId());
                    } else {
                        log.info("[\u79fb\u9664\u73a9\u5bb6] \u623f\u4e3b {} \u662f\u6700\u540e\u4e00\u4e2a\u79bb\u5f00\u7684\u73a9\u5bb6\uff0c\u623f\u95f4\u7b49\u5f85\u91cd\u8fde", (Object)removedPlayer.getPlayerId());
                    }
                }
                this.saveRoom(room);
                this.broadcastRoomState(roomNumber);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void handleReturnToRoom(String roomNumber, String requestingSessionId) {
        GameRoom room;
        GameRoom gameRoom = room = this.findRoomOrFail(roomNumber);
        synchronized (gameRoom) {
            Player requestingPlayer = (Player)room.getPlayers().get(requestingSessionId);
            if (requestingPlayer == null) {
                log.warn("\u8fd4\u56de\u623f\u95f4\u5931\u8d25\uff1aSession {} \u4e0d\u5728\u623f\u95f4 {} \u4e2d", (Object)requestingSessionId, (Object)roomNumber);
                throw new RuntimeException("\u60a8\u4e0d\u5728\u623f\u95f4\u4e2d");
            }
            if (room.isGameStarted()) {
                this.awlFjGameService.resetGame(room, requestingPlayer.getPlayerId());
                log.info("\u623f\u95f4 {} \u6e38\u620f\u5df2\u91cd\u7f6e\uff0c\u73a9\u5bb6 {} \u8fd4\u56de\u623f\u95f4\u51c6\u5907\u4e0b\u4e00\u5c40", (Object)roomNumber, (Object)requestingPlayer.getPlayerId());
            }
            this.saveRoom(room);
        }
        this.broadcastRoomState(roomNumber);
    }

    public void broadcastRoomState(String roomNumber) {
        GameRoom room = (GameRoom)this.gameRoomRedisTemplate.opsForValue().get((Object)(ROOM_KEY_PREFIX + roomNumber));
        if (room != null) {
            this.messagingTemplate.convertAndSend((Object)("/topic/room/" + roomNumber), (Object)room);
        } else {
            log.warn("\u5c1d\u8bd5\u5e7f\u64ad\u4e0d\u5b58\u5728\u7684\u623f\u95f4 {}", (Object)roomNumber);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void kickPlayer(String roomNumber, String requesterSessionId, String targetPlayerId) {
        GameRoom room;
        GameRoom gameRoom = room = this.findRoomOrFail(roomNumber);
        synchronized (gameRoom) {
            Player requester = (Player)room.getPlayers().get(requesterSessionId);
            if (requester == null || !requester.getPlayerId().equals(room.getHostPlayerId())) {
                throw new RuntimeException("\u53ea\u6709\u623f\u4e3b\u624d\u80fd\u8e22\u4eba");
            }
            if (requester.getPlayerId().equals(targetPlayerId)) {
                throw new RuntimeException("\u623f\u4e3b\u4e0d\u80fd\u8e22\u81ea\u5df1");
            }
            Player targetPlayer = null;
            String targetSessionId = null;
            for (Map.Entry entry : room.getPlayers().entrySet()) {
                if (!((Player)entry.getValue()).getPlayerId().equals(targetPlayerId)) continue;
                targetPlayer = (Player)entry.getValue();
                targetSessionId = (String)entry.getKey();
                break;
            }
            if (targetPlayer == null || targetSessionId == null) {
                throw new RuntimeException("\u672a\u627e\u5230\u5728\u7ebf\u7684\u76ee\u6807\u73a9\u5bb6");
            }
            log.info("\u623f\u4e3b {} \u8e22\u51fa\u73a9\u5bb6 {}", (Object)requesterSessionId, (Object)targetPlayerId);
            room.getPlayers().remove(targetSessionId);
            room.getInitialPlayers().values().removeIf(p -> p.getPlayerId().equals(targetPlayerId));
            this.removePlayerInternal(room, roomNumber, targetPlayer, targetSessionId);
            this.messagingTemplate.convertAndSendToUser(targetSessionId, "/queue/errors", (Object)Map.of((Object)"error", (Object)"\u60a8\u5df2\u88ab\u623f\u4e3b\u79fb\u51fa\u623f\u95f4"));
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void transferHost(String roomNumber, String requesterSessionId, String targetPlayerId) {
        GameRoom room;
        GameRoom gameRoom = room = this.findRoomOrFail(roomNumber);
        synchronized (gameRoom) {
            Player requester = (Player)room.getPlayers().get(requesterSessionId);
            if (requester == null || !requester.getPlayerId().equals(room.getHostPlayerId())) {
                throw new RuntimeException("\u53ea\u6709\u623f\u4e3b\u624d\u80fd\u8f6c\u8ba9\u6743\u9650");
            }
            if (requester.getPlayerId().equals(targetPlayerId)) {
                return;
            }
            boolean targetExists = room.getPlayers().values().stream().anyMatch(p -> p.getPlayerId().equals(targetPlayerId));
            if (!targetExists) {
                throw new RuntimeException("\u672a\u627e\u5230\u5728\u7ebf\u7684\u76ee\u6807\u73a9\u5bb6");
            }
            log.info("\u623f\u4e3b {} \u8f6c\u8ba9\u6743\u9650\u7ed9 {}", (Object)requester.getPlayerId(), (Object)targetPlayerId);
            room.setHostPlayerId(targetPlayerId);
            this.saveRoom(room);
            this.broadcastRoomState(roomNumber);
        }
    }

    private GameRoom findRoomOrFail(String roomNumber) {
        GameRoom room = (GameRoom)this.gameRoomRedisTemplate.opsForValue().get((Object)(ROOM_KEY_PREFIX + roomNumber));
        if (room == null) {
            log.warn("\u623f\u95f4 {} \u4e0d\u5b58\u5728", (Object)roomNumber);
            throw new RuntimeException("\u623f\u95f4 " + roomNumber + " \u4e0d\u5b58\u5728\u6216\u5df2\u89e3\u6563");
        }
        return room;
    }

    public void saveRoom(GameRoom room) {
        if (room == null || room.getRoomNumber() == null) {
            return;
        }
        String roomKey = ROOM_KEY_PREFIX + room.getRoomNumber();
        this.gameRoomRedisTemplate.opsForValue().set((Object)roomKey, (Object)room, 2L, TimeUnit.HOURS);
        log.debug("\u623f\u95f4 {} \u5df2\u4fdd\u5b58\uff0c\u8fc7\u671f\u65f6\u95f4: {} \u5c0f\u65f6", (Object)room.getRoomNumber(), (Object)2L);
    }

    private void deleteRoom(String roomNumber) {
        this.gameRoomRedisTemplate.delete((Object)(ROOM_KEY_PREFIX + roomNumber));
        log.info("\u623f\u95f4 {} \u5df2\u5220\u9664", (Object)roomNumber);
    }

    private void putSessionRoomMapping(String sessionId, String roomNumber) {
        if (sessionId == null || roomNumber == null) {
            return;
        }
        this.stringRedisTemplate.opsForValue().set((Object)("avalon:session:" + sessionId), (Object)roomNumber, 2L, TimeUnit.HOURS);
    }

    private void deleteSessionRoomMapping(String sessionId) {
        if (sessionId == null) {
            return;
        }
        this.stringRedisTemplate.delete((Object)("avalon:session:" + sessionId));
    }

    private void putPlayerSessionMapping(String playerId, String sessionId) {
        if (playerId == null || sessionId == null) {
            return;
        }
        this.stringRedisTemplate.opsForValue().set((Object)("avalon:player:" + playerId), (Object)sessionId, 2L, TimeUnit.HOURS);
    }

    private String getPlayerSessionMapping(String playerId) {
        if (playerId == null) {
            return null;
        }
        return (String)this.stringRedisTemplate.opsForValue().get((Object)("avalon:player:" + playerId));
    }

    private void deletePlayerSessionMapping(String playerId) {
        if (playerId == null) {
            return;
        }
        this.stringRedisTemplate.delete((Object)("avalon:player:" + playerId));
    }

    public void scheduleSeatSelectionKick(String roomNumber, String playerId, String sessionId) {
        this.cancelSeatSelectionKick(playerId);
        Runnable kickTask = () -> this.kickPlayerForInactivity(roomNumber, playerId, sessionId);
        ScheduledFuture task = this.taskScheduler.schedule(kickTask, Instant.now().plusMillis(this.seatSelectTimeoutMs));
        this.pendingSeatSelections.put(playerId, task);
    }

    public void cancelSeatSelectionKick(String playerId) {
        if (playerId == null) {
            return;
        }
        ScheduledFuture task = (ScheduledFuture)this.pendingSeatSelections.remove(playerId);
        if (task != null) {
            task.cancel(false);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void kickPlayerForInactivity(String roomNumber, String playerId, String sessionId) {
        GameRoom room = (GameRoom)this.gameRoomRedisTemplate.opsForValue().get((Object)(ROOM_KEY_PREFIX + roomNumber));
        if (room == null) {
            log.debug("AFK \u68c0\u67e5\uff1a\u623f\u95f4 {} \u5df2\u4e0d\u5b58\u5728", (Object)roomNumber);
            return;
        }
        GameRoom gameRoom = room;
        synchronized (gameRoom) {
            Player player = (Player)room.getPlayers().get(sessionId);
            if (player != null && player.getPlayerId().equals(playerId) && player.getSeatNumber() == 0) {
                log.warn("\u73a9\u5bb6 {} \u56e0\u672a\u9009\u5ea7\u88ab\u8e22\u51fa\u623f\u95f4 {}", (Object)playerId, (Object)roomNumber);
                room.getPlayers().remove(sessionId);
                room.getInitialPlayers().values().removeIf(p -> p.getPlayerId().equals(playerId));
                this.removePlayerInternal(room, roomNumber, player, sessionId);
                this.messagingTemplate.convertAndSendToUser(sessionId, "/queue/errors", (Object)Map.of((Object)"error", (Object)"\u60a8\u56e0\u957f\u65f6\u95f4\u672a\u9009\u5ea7\u88ab\u79fb\u51fa\u623f\u95f4"));
            }
        }
    }

    private void removePlayerInternal(GameRoom room, String roomNumber, Player removedPlayer, String removedSessionId) {
        boolean roomDisbanded = false;
        if (room.getPlayers().isEmpty()) {
            if (!room.isGameStarted() || room.getInitialPlayers().isEmpty()) {
                this.deleteRoom(roomNumber);
                roomDisbanded = true;
            } else {
                log.info("\u623f\u95f4 {} \u6e38\u620f\u5df2\u5f00\u59cb\uff0c\u6240\u6709\u73a9\u5bb6\u5747\u5df2\u65ad\u7ebf", (Object)roomNumber);
            }
        }
        if (!roomDisbanded) {
            if (removedPlayer.getPlayerId().equals(room.getHostPlayerId())) {
                Optional newHost = room.getPlayers().values().stream().findFirst();
                if (newHost.isPresent()) {
                    room.setHostPlayerId(((Player)newHost.get()).getPlayerId());
                    log.info("\u623f\u4e3b {} \u88ab\u79fb\u9664\uff0c\u65b0\u623f\u4e3b\u662f {}", (Object)removedPlayer.getPlayerId(), (Object)((Player)newHost.get()).getPlayerId());
                } else if (room.isGameStarted() && !room.getInitialPlayers().isEmpty()) {
                    Player newHostFromOffline = (Player)room.getInitialPlayers().values().iterator().next();
                    room.setHostPlayerId(newHostFromOffline.getPlayerId());
                    log.info("\u623f\u4e3b {} \u88ab\u79fb\u9664\uff0c\u65b0\u623f\u4e3b\u6307\u5b9a\u4e3a\u79bb\u7ebf\u73a9\u5bb6 {}", (Object)removedPlayer.getPlayerId(), (Object)newHostFromOffline.getPlayerId());
                }
            }
            this.saveRoom(room);
            this.broadcastRoomState(roomNumber);
        }
    }

    private String normalizePreferredRole(String preferredRole) {
        if (preferredRole == null) {
            return PREFERRED_ROLE_CLEAR_TOKEN;
        }
        if (PREFERRED_ROLE_CLEAR_TOKEN.equals(preferredRole)) {
            return PREFERRED_ROLE_CLEAR_TOKEN;
        }
        String trimmed = preferredRole.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (ALLOWED_FORCED_ROLES.contains(trimmed)) {
            return trimmed;
        }
        log.warn("\u6536\u5230\u672a\u652f\u6301\u7684\u5f3a\u5236\u8eab\u4efd\u8bf7\u6c42: {}", (Object)preferredRole);
        return null;
    }

    private void assignPreferredRole(Player player, String preferredRole, String context) {
        if (player == null) {
            return;
        }
        String normalized = preferredRole;
        if (normalized != null && normalized.trim().isEmpty()) {
            normalized = PREFERRED_ROLE_CLEAR_TOKEN;
        }
        if (PREFERRED_ROLE_CLEAR_TOKEN.equals(normalized)) {
            player.setPreferredRole(null);
            log.info("[{}] \u5df2\u6e05\u7a7a\u73a9\u5bb6 {} \u7684\u9996\u9009\u8eab\u4efd", (Object)context, (Object)player.getPlayerId());
            return;
        }
        if (PREFERRED_ROLE_CLEAR_TOKEN.equals(normalized = this.normalizePreferredRole(normalized))) {
            player.setPreferredRole(null);
            log.info("[{}] \u5df2\u6e05\u7a7a\u73a9\u5bb6 {} \u7684\u9996\u9009\u8eab\u4efd", (Object)context, (Object)player.getPlayerId());
            return;
        }
        if (normalized != null) {
            player.setPreferredRole(normalized);
            log.info("[{}] \u5df2\u4e3a\u73a9\u5bb6 {} \u8bbe\u7f6e\u9996\u9009\u8eab\u4efd {}", new Object[]{context, player.getPlayerId(), normalized});
        }
    }

    private void applyPreferredRoleOverride(Player player, String preferredRole) {
        this.assignPreferredRole(player, preferredRole, "\u91cd\u8fde");
    }

    public RoomService(RedisTemplate<String, GameRoom> gameRoomRedisTemplate, SimpMessagingTemplate messagingTemplate, AwlFjGameService awlFjGameService, TaskScheduler taskScheduler, StringRedisTemplate stringRedisTemplate) {
        this.gameRoomRedisTemplate = gameRoomRedisTemplate;
        this.messagingTemplate = messagingTemplate;
        this.awlFjGameService = awlFjGameService;
        this.taskScheduler = taskScheduler;
        this.stringRedisTemplate = stringRedisTemplate;
    }
}

