package cn.game.board.service;

import cn.game.board.model.avalon.dto.GameRoom;
import cn.game.board.model.avalon.dto.Player;
import cn.game.board.model.avalon.dto.SpectatorPlayerView;
import cn.game.board.model.avalon.dto.SpectatorRoomView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

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

/**
 * 游戏房间管理服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RoomService {

    public static final String ROOM_KEY_PREFIX = "avalon:room:";
    public static final String PLAYER_SEAT_KEY_PREFIX = "avalon:player:seat:";
    public static final String PREFERRED_ROLE_CLEAR_TOKEN = "__CLEAR__";
    private static final long ROOM_EXPIRATION_HOURS = 2;
    private static final Set<String> ALLOWED_FORCED_ROLES = Set.of("梅林", "派西维尔", "亚瑟的忠臣");
    private static final Set<String> EVIL_IDENTITIES_FOR_SPECTATOR = Set.of("莫甘娜", "刺客", "莫德雷德的爪牙", "莫德雷德", "奥伯伦", "兰斯洛特（红）");

    private final RedisTemplate<String, GameRoom> gameRoomRedisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final AwlFjGameService awlFjGameService;
    private final TaskScheduler taskScheduler;
    private final Map<String, ScheduledFuture<?>> pendingSeatSelections = new ConcurrentHashMap<>();
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.game.afk.seat-select-timeout-ms:30000}")
    private long seatSelectTimeoutMs;

    public Set<String> getActiveRoomKeys() {
        return gameRoomRedisTemplate.keys(ROOM_KEY_PREFIX + "*");
    }

    public GameRoom getRoomByKey(String roomKey) {
        return gameRoomRedisTemplate.opsForValue().get(roomKey);
    }

    /**
     * 构建观战视图
     */
    public SpectatorRoomView buildSpectatorView(String roomNumber) {
        GameRoom room = findRoomOrFail(roomNumber);
        SpectatorRoomView view = new SpectatorRoomView();
        view.setRoomNumber(roomNumber);
        view.setGameStarted(room.isGameStarted());
        view.setTotalPlayers(room.getTotalPlayers());
        view.setCurrentLeaderSeat(room.getCurrentLeaderSeat());
        view.setCurrentMissionRound(room.getCurrentMissionRound());

        Map<String, Player> sourcePlayers = null;
        if (room.isGameStarted() && room.getInitialPlayers() != null && !room.getInitialPlayers().isEmpty()) {
            sourcePlayers = room.getInitialPlayers();
            log.debug("[观战] 房间 {} 游戏已开始，使用 initialPlayers，共 {} 人", roomNumber, sourcePlayers.size());
        } else {
            sourcePlayers = room.getPlayers();
            log.debug("[观战] 房间 {} 游戏未开始，使用在线玩家列表，共 {} 人", roomNumber, sourcePlayers != null ? sourcePlayers.size() : 0);
        }
        if (sourcePlayers == null || sourcePlayers.isEmpty()) {
            sourcePlayers = room.getPlayers();
            log.debug("[观战] 房间 {} 使用后备玩家列表，共 {} 人", roomNumber, sourcePlayers != null ? sourcePlayers.size() : 0);
        }

        List<SpectatorPlayerView> playerViews = new ArrayList<>();
        if (sourcePlayers != null && !sourcePlayers.isEmpty()) {
            playerViews = sourcePlayers.values().stream()
                    .filter(player -> player.getSeatNumber() > 0)
                    .sorted(Comparator.comparingInt(Player::getSeatNumber))
                    .map(player -> {
                        String identity = player.getIdentity();
                        String camp = identity != null && EVIL_IDENTITIES_FOR_SPECTATOR.contains(identity) ? "evil" : "good";
                        return new SpectatorPlayerView(player.getSeatNumber(), player.getPlayerId(), identity, camp);
                    })
                    .collect(Collectors.toList());
        }

        view.setPlayers(playerViews);
        log.info("[观战] 房间 {} 观战视图构建完成：游戏状态={}, 总人数={}, 玩家数={}",
                roomNumber, room.isGameStarted(), room.getTotalPlayers(), playerViews.size());
        return view;
    }

    /**
     * 创建房间
     */
    public GameRoom createRoom(String roomNumber, int totalPlayers, String hostSessionId, String preferredRole) {
        if (totalPlayers < 5 || totalPlayers > 12) {
            throw new RuntimeException("不支持的人数: " + totalPlayers + " (仅支持 5-12 人)");
        }

        String roomKey = ROOM_KEY_PREFIX + roomNumber;
        GameRoom existingRoom = gameRoomRedisTemplate.opsForValue().get(roomKey);
        if (existingRoom != null) {
            throw new RuntimeException("房间 " + roomNumber + " 已存在");
        }

        log.info("创建房间 {} ({}人局, sessionId: {})", roomNumber, totalPlayers, hostSessionId);
        GameRoom room = new GameRoom(roomNumber, totalPlayers, null);
        Player hostPlayer = new Player(hostSessionId);
        assignPreferredRole(hostPlayer, preferredRole, "创建房间");
        room.getPlayers().put(hostSessionId, hostPlayer);
        room.setHostPlayerId(hostPlayer.getPlayerId());

        saveRoom(room);
        putSessionRoomMapping(hostSessionId, roomNumber);
        putPlayerSessionMapping(hostPlayer.getPlayerId(), hostSessionId);
        scheduleSeatSelectionKick(roomNumber, hostPlayer.getPlayerId(), hostSessionId);
        broadcastRoomState(roomNumber);
        return room;
    }

    /**
     * 玩家加入房间
     */
    public GameRoom joinRoom(String roomNumber, String playerSessionId, String preferredRole) {
        GameRoom room = findRoomOrFail(roomNumber);
        if (room.getTotalPlayers() < 5 || room.getTotalPlayers() > 12) {
            throw new RuntimeException("房间配置错误");
        }

        synchronized (room) {
            if (room.getPlayers().containsKey(playerSessionId)) {
                log.warn("玩家 {} 已在房间 {} 中", playerSessionId, roomNumber);
                return room;
            }

            if (room.getPlayers().size() >= room.getTotalPlayers()) {
                throw new RuntimeException("房间 " + roomNumber + " 已满");
            }

                Player newPlayer = new Player(playerSessionId);
            assignPreferredRole(newPlayer, preferredRole, "加入房间");
            room.getPlayers().put(playerSessionId, newPlayer);
            log.info("玩家 {} 加入房间 {}", playerSessionId, roomNumber);

                saveRoom(room);
                putSessionRoomMapping(playerSessionId, roomNumber);
                putPlayerSessionMapping(newPlayer.getPlayerId(), playerSessionId);
                scheduleSeatSelectionKick(roomNumber, newPlayer.getPlayerId(), playerSessionId);
            }

        broadcastRoomState(roomNumber);
        return room;
    }

    /**
     * 处理玩家断线重连
     */
    public void rejoinWithId(String roomNumber, String playerId, String newSessionId, String preferredRole) {
        GameRoom room = findRoomOrFail(roomNumber);
        synchronized (room) {
            Player playerToRejoin = null;
            String oldSessionId = null;

            Optional<Player> playerFromSnapshot = room.getInitialPlayers() != null && !room.getInitialPlayers().isEmpty()
                    ? room.getInitialPlayers().values().stream()
                            .filter(p -> p.getPlayerId().equals(playerId))
                            .findFirst()
                    : Optional.empty();

            if (playerFromSnapshot.isPresent()) {
                playerToRejoin = playerFromSnapshot.get();
                oldSessionId = playerToRejoin.getSessionId();

                    Optional<Map.Entry<String, Player>> onlineSamePlayer = room.getPlayers().entrySet().stream()
                            .filter(e -> e.getValue().getPlayerId().equals(playerId))
                            .findFirst();
                onlineSamePlayer.ifPresent(entry -> {
                    String existingSession = entry.getKey();
                        room.getPlayers().remove(existingSession);
                        deleteSessionRoomMapping(existingSession);
                        log.warn("玩家 {} 已在线，替换会话 {} -> {}", playerId, existingSession, newSessionId);
                });

                playerToRejoin.setSessionId(newSessionId);
                playerToRejoin.setReady(false);
                room.getPlayers().put(newSessionId, playerToRejoin);
                applyPreferredRoleOverride(playerToRejoin, preferredRole);
                restoreSeatFromRedis(room, playerToRejoin);
                cancelSeatSelectionKick(playerToRejoin.getPlayerId());
                if (playerToRejoin.getSeatNumber() == 0) {
                    scheduleSeatSelectionKick(roomNumber, playerToRejoin.getPlayerId(), newSessionId);
                }

                putSessionRoomMapping(newSessionId, roomNumber);
                putPlayerSessionMapping(playerToRejoin.getPlayerId(), newSessionId);
                saveRoom(room);

                if (room.isGameStarted()) {
                    log.info("玩家 {} 重连房间 {} 成功 (游戏进行中, 身份: {}, Session: {} -> {})", 
                            playerId, roomNumber, playerToRejoin.getIdentity(), oldSessionId, newSessionId);
                    awlFjGameService.sendIdentityToPlayer(room, playerToRejoin, newSessionId);
                } else {
                    log.info("玩家 {} 重连房间 {} 成功 (游戏未开始, Session: {} -> {})", 
                            playerId, roomNumber, oldSessionId, newSessionId);
                }
            } else {
                Optional<Map.Entry<String, Player>> playerEntry = room.getPlayers().entrySet().stream()
                        .filter(entry -> entry.getValue().getPlayerId().equals(playerId))
                        .findFirst();

                if (playerEntry.isPresent()) {
                    log.info("玩家 {} 在在线列表中，更新 Session", playerId);
                    playerToRejoin = playerEntry.get().getValue();
                    oldSessionId = playerEntry.get().getKey();
                    if (!oldSessionId.equals(newSessionId)) {
                        room.getPlayers().remove(oldSessionId);
                        playerToRejoin.setSessionId(newSessionId);
                        room.getPlayers().put(newSessionId, playerToRejoin);
                    }
                    playerToRejoin.setReady(false);
                    applyPreferredRoleOverride(playerToRejoin, preferredRole);
                    restoreSeatFromRedis(room, playerToRejoin);
                    cancelSeatSelectionKick(playerId);
                    if (playerToRejoin.getSeatNumber() == 0) {
                        scheduleSeatSelectionKick(roomNumber, playerId, newSessionId);
                    }
                    deleteSessionRoomMapping(oldSessionId);
                    putSessionRoomMapping(newSessionId, roomNumber);
                    putPlayerSessionMapping(playerId, newSessionId);
                    saveRoom(room);
                    } else {
                        Optional<Player> initialPlayer = (room.getInitialPlayers() != null && !room.getInitialPlayers().isEmpty())
                                ? room.getInitialPlayers().values().stream()
                                        .filter(p -> p.getPlayerId().equals(playerId))
                                        .findFirst()
                                : Optional.empty();
                        
                        if (initialPlayer.isPresent()) {
                            log.info("玩家 {} 在初始玩家列表中，恢复玩家到在线列表", playerId);
                            playerToRejoin = initialPlayer.get();
                            playerToRejoin.setSessionId(newSessionId);
                            playerToRejoin.setReady(false);
                            room.getPlayers().put(newSessionId, playerToRejoin);
                        applyPreferredRoleOverride(playerToRejoin, preferredRole);
                        restoreSeatFromRedis(room, playerToRejoin);
                            cancelSeatSelectionKick(playerId);
                            if (playerToRejoin.getSeatNumber() == 0) {
                                scheduleSeatSelectionKick(roomNumber, playerToRejoin.getPlayerId(), newSessionId);
                            }
                            
                            putSessionRoomMapping(newSessionId, roomNumber);
                            putPlayerSessionMapping(playerToRejoin.getPlayerId(), newSessionId);
                            saveRoom(room);
                    } else if (!room.isGameStarted()) {
                                if (room.getPlayers().size() >= room.getTotalPlayers()) {
                                    log.warn("玩家 {} 重连房间 {} 失败：房间已满", playerId, roomNumber);
                                    throw new RuntimeException("房间 " + roomNumber + " 已满，无法重连");
                                }
                                
                                Player newPlayer = new Player(newSessionId);
                        newPlayer.setPlayerId(playerId);
                        applyPreferredRoleOverride(newPlayer, preferredRole);
                        restoreSeatFromRedis(room, newPlayer);
                                room.getPlayers().put(newSessionId, newPlayer);
                                
                                putSessionRoomMapping(newSessionId, roomNumber);
                                putPlayerSessionMapping(playerId, newSessionId);
                        if (newPlayer.getSeatNumber() == 0) {
                                scheduleSeatSelectionKick(roomNumber, playerId, newSessionId);
                        }
                                saveRoom(room);
                                log.info("玩家 {} 重连房间 {} 成功 (游戏未开始, 新Session: {})", playerId, roomNumber, newSessionId);
                            } else {
                                log.warn("重连失败：未找到玩家 {}，既不在在线列表也不在初始玩家列表，且游戏已开始", playerId);
                                throw new RuntimeException("您已不在房间 " + roomNumber + " 中");
                        }
                    }
            }
        }

        broadcastRoomState(roomNumber);
    }

    /**
     * 玩家选择座位
     */
    public void selectSeat(String roomNumber, String playerSessionId, int seatNumber) {
        GameRoom room = findRoomOrFail(roomNumber);
        synchronized (room) {
            if (seatNumber < 1 || seatNumber > room.getTotalPlayers()) {
                throw new RuntimeException("无效的座位号: " + seatNumber);
            }

            for (Player p : room.getPlayers().values()) {
                if (p.getSeatNumber() == seatNumber && !p.getSessionId().equals(playerSessionId)) {
                    throw new RuntimeException("座位 " + seatNumber + " 已被占用");
                }
            }

            Player player = room.getPlayers().get(playerSessionId);
            if (player != null) {
                if (player.getSeatNumber() != seatNumber) {
                    log.info("玩家 {} 选择座位 {}", playerSessionId, seatNumber);
                    cancelSeatSelectionKick(player.getPlayerId());
                    player.setSeatNumber(seatNumber);
                    player.setReady(false);
                    saveRoom(room);
                    broadcastRoomState(roomNumber);
                }
            } else {
                log.warn("玩家 {} 不在房间 {}", playerSessionId, roomNumber);
            }
        }
    }

    /**
     * 设置玩家准备状态
     */
    public void setReady(String roomNumber, String playerSessionId, boolean isReady) {
        GameRoom room = findRoomOrFail(roomNumber);
        synchronized (room) {
            Player player = room.getPlayers().get(playerSessionId);
            if (player != null && player.getSeatNumber() != 0) {
                if (player.isReady() != isReady) {
                    log.info("玩家 {} 设置准备状态: {}", playerSessionId, isReady);
                    player.setReady(isReady);
                    awlFjGameService.checkGameStart(room);
                    saveRoom(room);
                    broadcastRoomState(roomNumber);
                }
            } else {
                if (player == null) {
                    log.warn("玩家 {} 不在房间 {}", playerSessionId, roomNumber);
                } else {
                    log.warn("玩家 {} 尚未选座", playerSessionId);
                }
            }
        }
    }

    /**
     * 更新玩家首选身份
     */
    public void updatePreferredRole(String roomNumber, String requesterSessionId, String playerId, String preferredRole) {
        GameRoom room = findRoomOrFail(roomNumber);
        synchronized (room) {
            Player targetPlayer = room.getPlayers().get(requesterSessionId);

            if (targetPlayer == null && playerId != null) {
                Optional<Map.Entry<String, Player>> entry = room.getPlayers().entrySet().stream()
                        .filter(e -> e.getValue().getPlayerId().equals(playerId))
                        .findFirst();
                if (entry.isPresent()) {
                    String existingSession = entry.get().getKey();
                    targetPlayer = entry.get().getValue();
                    if (!existingSession.equals(requesterSessionId)) {
                        room.getPlayers().remove(existingSession);
                        targetPlayer.setSessionId(requesterSessionId);
                        room.getPlayers().put(requesterSessionId, targetPlayer);
                        deleteSessionRoomMapping(existingSession);
                        putSessionRoomMapping(requesterSessionId, roomNumber);
                        putPlayerSessionMapping(targetPlayer.getPlayerId(), requesterSessionId);
                        log.info("[更新首选身份] 玩家 {} sessionId 由 {} 替换为 {}", targetPlayer.getPlayerId(), existingSession, requesterSessionId);
                    }
                }
            }

            if (targetPlayer == null && playerId != null && room.getInitialPlayers() != null && !room.getInitialPlayers().isEmpty()) {
                targetPlayer = room.getInitialPlayers().values().stream()
                        .filter(p -> p.getPlayerId().equals(playerId))
                        .findFirst()
                        .orElse(null);
                if (targetPlayer != null) {
                    log.info("[更新首选身份] 玩家 {} 当前不在线，更新 initialPlayers 记录", targetPlayer.getPlayerId());
                }
            }

            if (targetPlayer == null) {
                throw new RuntimeException("玩家不在房间中，无法更新首选身份");
            }

            assignPreferredRole(targetPlayer, preferredRole, "更新首选身份");
            saveRoom(room);
        }
    }

    /**
     * 移除玩家
     */
    public void removePlayer(String roomNumber, String playerSessionId) {
        GameRoom room = findRoomOrFail(roomNumber);
        synchronized (room) {
            Player removedPlayer = room.getPlayers().remove(playerSessionId);
            if (removedPlayer == null) {
                log.debug("玩家 {} 已不在在线列表中", playerSessionId);
                return;
            }

            persistSeatNumberForReconnect(room, removedPlayer);
            cancelSeatSelectionKick(removedPlayer.getPlayerId());
            log.info("玩家 {} 断开连接 (房间: {}, Session: {})", removedPlayer.getPlayerId(), roomNumber, playerSessionId);

            // 删除 session 到 room 的映射
            deleteSessionRoomMapping(playerSessionId);
            
            // 删除 playerId 到 sessionId 的映射
            // 注意：即使删除此映射，重连时也能从 initialPlayers 中恢复玩家信息
            String mapped = getPlayerSessionMapping(removedPlayer.getPlayerId());
            if (playerSessionId.equals(mapped)) {
                deletePlayerSessionMapping(removedPlayer.getPlayerId());
                log.debug("已删除玩家 {} 的 session 映射", removedPlayer.getPlayerId());
            }

            boolean roomDisbanded = false;

            if (room.getPlayers().isEmpty() && !room.isGameStarted()) {
                deleteRoom(roomNumber);
                log.info("房间 {} 已解散", roomNumber);
                roomDisbanded = true;
            } else if (room.getPlayers().isEmpty() && room.isGameStarted()) {
                log.info("房间 {} 所有玩家均已断线，等待重连", roomNumber);
            }

            if (!roomDisbanded) {
                if (removedPlayer.getPlayerId().equals(room.getHostPlayerId())) {
                    Optional<Player> newHost = room.getPlayers().values().stream().findFirst();
                    if (newHost.isPresent()) {
                        room.setHostPlayerId(newHost.get().getPlayerId());
                        log.info("房主 {} 断线，新房主是 {}", removedPlayer.getPlayerId(), newHost.get().getPlayerId());
                    } else {
                        log.info("房主 {} 是最后一个离开的玩家，房间等待重连", removedPlayer.getPlayerId());
                    }
                }
                saveRoom(room);
                broadcastRoomState(roomNumber);
            }
        }
    }

    /**
     * 处理返回房间请求
     */
    public void handleReturnToRoom(String roomNumber, String requestingSessionId) {
        GameRoom room = findRoomOrFail(roomNumber);
        synchronized (room) {
            Player requestingPlayer = room.getPlayers().get(requestingSessionId);
            if (requestingPlayer == null) {
                log.warn("返回房间失败：Session {} 不在房间 {} 中", requestingSessionId, roomNumber);
                throw new RuntimeException("您不在房间中");
            }

            // 重置游戏状态（让玩家可以重新开始一局）
            if (room.isGameStarted()) {
                awlFjGameService.resetGame(room, requestingPlayer.getPlayerId());
                log.info("房间 {} 游戏已重置，玩家 {} 返回房间准备下一局", roomNumber, requestingPlayer.getPlayerId());
            }

            saveRoom(room);
        }

        broadcastRoomState(roomNumber);
    }

    /**
     * 广播房间状态
     */
    public void broadcastRoomState(String roomNumber) {
        GameRoom room = gameRoomRedisTemplate.opsForValue().get(ROOM_KEY_PREFIX + roomNumber);
        if (room != null) {
            messagingTemplate.convertAndSend("/topic/room/" + roomNumber, room);
        } else {
            log.warn("尝试广播不存在的房间 {}", roomNumber);
        }
    }

    /**
     * 房主踢出玩家
     */
    public void kickPlayer(String roomNumber, String requesterSessionId, String targetPlayerId) {
        GameRoom room = findRoomOrFail(roomNumber);
        synchronized (room) {
            Player requester = room.getPlayers().get(requesterSessionId);
            if (requester == null || !requester.getPlayerId().equals(room.getHostPlayerId())) {
                throw new RuntimeException("只有房主才能踢人");
            }
            if (requester.getPlayerId().equals(targetPlayerId)) {
                throw new RuntimeException("房主不能踢自己");
            }

            Player targetPlayer = null;
            String targetSessionId = null;
            for (Map.Entry<String, Player> entry : room.getPlayers().entrySet()) {
                if (entry.getValue().getPlayerId().equals(targetPlayerId)) {
                    targetPlayer = entry.getValue();
                    targetSessionId = entry.getKey();
                    break;
                }
            }

            if (targetPlayer != null && targetSessionId != null) {
                log.info("房主 {} 踢出玩家 {}", requesterSessionId, targetPlayerId);
                room.getPlayers().remove(targetSessionId);
                room.getInitialPlayers().values().removeIf(p -> p.getPlayerId().equals(targetPlayerId));
                removePlayerInternal(room, roomNumber, targetPlayer, targetSessionId);
                messagingTemplate.convertAndSendToUser(targetSessionId, "/queue/errors", Map.of("error", "您已被房主移出房间"));
            } else {
                throw new RuntimeException("未找到在线的目标玩家");
            }
        }
    }

    /**
     * 房主转让权限
     */
    public void transferHost(String roomNumber, String requesterSessionId, String targetPlayerId) {
        GameRoom room = findRoomOrFail(roomNumber);
        synchronized (room) {
            Player requester = room.getPlayers().get(requesterSessionId);
            if (requester == null || !requester.getPlayerId().equals(room.getHostPlayerId())) {
                throw new RuntimeException("只有房主才能转让权限");
            }
            if (requester.getPlayerId().equals(targetPlayerId)) {
                return;
            }

            boolean targetExists = room.getPlayers().values().stream()
                    .anyMatch(p -> p.getPlayerId().equals(targetPlayerId));

            if (targetExists) {
                log.info("房主 {} 转让权限给 {}", requester.getPlayerId(), targetPlayerId);
                room.setHostPlayerId(targetPlayerId);
                saveRoom(room);
                broadcastRoomState(roomNumber);
            } else {
                throw new RuntimeException("未找到在线的目标玩家");
            }
        }
    }

    private GameRoom findRoomOrFail(String roomNumber) {
        GameRoom room = gameRoomRedisTemplate.opsForValue().get(ROOM_KEY_PREFIX + roomNumber);
        if (room == null) {
            log.warn("房间 {} 不存在", roomNumber);
            throw new RuntimeException("房间 " + roomNumber + " 不存在或已解散");
        }
        return room;
    }

    public void saveRoom(GameRoom room) {
        if (room == null || room.getRoomNumber() == null) return;
        String roomKey = ROOM_KEY_PREFIX + room.getRoomNumber();
        gameRoomRedisTemplate.opsForValue().set(roomKey, room, ROOM_EXPIRATION_HOURS, TimeUnit.HOURS);
    }

    private void deleteRoom(String roomNumber) {
        gameRoomRedisTemplate.delete(ROOM_KEY_PREFIX + roomNumber);
        log.info("房间 {} 已删除", roomNumber);
    }

    private void putSessionRoomMapping(String sessionId, String roomNumber) {
        if (sessionId == null || roomNumber == null) return;
        stringRedisTemplate.opsForValue().set("avalon:session:" + sessionId, roomNumber, ROOM_EXPIRATION_HOURS, TimeUnit.HOURS);
    }

    private void deleteSessionRoomMapping(String sessionId) {
        if (sessionId == null) return;
        stringRedisTemplate.delete("avalon:session:" + sessionId);
    }

    private void putPlayerSessionMapping(String playerId, String sessionId) {
        if (playerId == null || sessionId == null) return;
        stringRedisTemplate.opsForValue().set("avalon:player:" + playerId, sessionId, ROOM_EXPIRATION_HOURS, TimeUnit.HOURS);
    }

    private String getPlayerSessionMapping(String playerId) {
        if (playerId == null) return null;
        return stringRedisTemplate.opsForValue().get("avalon:player:" + playerId);
    }

    private void deletePlayerSessionMapping(String playerId) {
        if (playerId == null) return;
        stringRedisTemplate.delete("avalon:player:" + playerId);
    }

    public void scheduleSeatSelectionKick(String roomNumber, String playerId, String sessionId) {
        cancelSeatSelectionKick(playerId);
        Runnable kickTask = () -> kickPlayerForInactivity(roomNumber, playerId, sessionId);
        ScheduledFuture<?> task = taskScheduler.schedule(kickTask, Instant.now().plusMillis(seatSelectTimeoutMs));
        pendingSeatSelections.put(playerId, task);
    }

    public void cancelSeatSelectionKick(String playerId) {
        if (playerId == null) return;
        ScheduledFuture<?> task = pendingSeatSelections.remove(playerId);
        if (task != null) {
            task.cancel(false);
        }
    }

    private void kickPlayerForInactivity(String roomNumber, String playerId, String sessionId) {
        GameRoom room = gameRoomRedisTemplate.opsForValue().get(ROOM_KEY_PREFIX + roomNumber);
        if (room == null) {
            log.debug("AFK 检查：房间 {} 已不存在", roomNumber);
            return;
        }

        synchronized (room) {
            Player player = room.getPlayers().get(sessionId);
            if (player != null && player.getPlayerId().equals(playerId)) {
                if (player.getSeatNumber() == 0) {
                    log.warn("玩家 {} 因未选座被踢出房间 {}", playerId, roomNumber);
                    room.getPlayers().remove(sessionId);
                    room.getInitialPlayers().values().removeIf(p -> p.getPlayerId().equals(playerId));
                    removePlayerInternal(room, roomNumber, player, sessionId);
                    messagingTemplate.convertAndSendToUser(sessionId, "/queue/errors", Map.of("error", "您因长时间未选座被移出房间"));
                }
            }
        }
        }

    private void removePlayerInternal(GameRoom room, String roomNumber, Player removedPlayer, String removedSessionId) {
        boolean roomDisbanded = false;

        if (room.getPlayers().isEmpty()) {
            if (!room.isGameStarted() || room.getInitialPlayers().isEmpty()) {
                deleteRoom(roomNumber);
                roomDisbanded = true;
            } else {
                log.info("房间 {} 游戏已开始，所有玩家均已断线", roomNumber);
            }
        }

        if (!roomDisbanded) {
            persistSeatNumberForReconnect(room, removedPlayer);
            if (removedPlayer.getPlayerId().equals(room.getHostPlayerId())) {
                Optional<Player> newHost = room.getPlayers().values().stream().findFirst();
                if (newHost.isPresent()) {
                    room.setHostPlayerId(newHost.get().getPlayerId());
                    log.info("房主 {} 被移除，新房主是 {}", removedPlayer.getPlayerId(), newHost.get().getPlayerId());
                } else if (room.isGameStarted() && !room.getInitialPlayers().isEmpty()) {
                        Player newHostFromOffline = room.getInitialPlayers().values().iterator().next();
                        room.setHostPlayerId(newHostFromOffline.getPlayerId());
                    log.info("房主 {} 被移除，新房主指定为离线玩家 {}", removedPlayer.getPlayerId(), newHostFromOffline.getPlayerId());
                }
            }
            saveRoom(room);
            broadcastRoomState(roomNumber);
        }
    }

    private void persistSeatNumberForReconnect(GameRoom room, Player player) {
        if (room == null || player == null) {
            return;
        }
        if (room.isGameStarted() || player.getSeatNumber() <= 0) {
            return;
        }
        String key = buildPlayerSeatKey(room.getRoomNumber(), player.getPlayerId());
        stringRedisTemplate.opsForValue().set(
                key,
                String.valueOf(player.getSeatNumber()),
                ROOM_EXPIRATION_HOURS,
                TimeUnit.HOURS);
        log.info("[移除玩家] 保存玩家座位号信息到 Redis: playerId={}, seatNumber={}, key={}",
                player.getPlayerId(), player.getSeatNumber(), key);
    }

    private void restoreSeatFromRedis(GameRoom room, Player player) {
        if (room == null || player == null || room.isGameStarted()) {
            return;
        }
        String key = buildPlayerSeatKey(room.getRoomNumber(), player.getPlayerId());
        String storedSeat = stringRedisTemplate.opsForValue().get(key);
        if (storedSeat == null) {
            return;
        }
        try {
            int seatNumber = Integer.parseInt(storedSeat);
            boolean seatTaken = room.getPlayers().values().stream()
                    .filter(p -> p != player)
                    .anyMatch(p -> p.getSeatNumber() == seatNumber);
            if (!seatTaken && seatNumber >= 1 && seatNumber <= room.getTotalPlayers()) {
                player.setSeatNumber(seatNumber);
                log.info("玩家 {} 从 Redis 恢复座位 {}", player.getPlayerId(), seatNumber);
            } else {
                log.warn("无法恢复玩家 {} 的座位 {}：已被占用或无效", player.getPlayerId(), seatNumber);
            }
        } catch (NumberFormatException ex) {
            log.warn("无法解析玩家 {} 的座位号 {}", player.getPlayerId(), storedSeat);
        } finally {
            stringRedisTemplate.delete(key);
        }
    }

    private String buildPlayerSeatKey(String roomNumber, String playerId) {
        return PLAYER_SEAT_KEY_PREFIX + roomNumber + ":" + playerId;
    }

    private void assignPreferredRole(Player player, String rawPreferredRole, String reason) {
        if (player == null) {
            return;
        }
        String normalized = normalizePreferredRole(rawPreferredRole);
        if (PREFERRED_ROLE_CLEAR_TOKEN.equals(normalized)) {
            if (player.getPreferredRole() != null) {
                player.setPreferredRole(null);
                log.info("[{}] 已清空玩家 {} 的首选身份", reason, player.getPlayerId());
            }
            return;
        }
        if (normalized != null) {
            player.setPreferredRole(normalized);
            log.info("[{}] 已为玩家 {} 设置首选身份 {}", reason, player.getPlayerId(), normalized);
        }
    }

    private void applyPreferredRoleOverride(Player player, String preferredRole) {
        assignPreferredRole(player, preferredRole, "重连");
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
        log.warn("收到未支持的强制身份请求: {}", preferredRole);
        return null;
    }
}
