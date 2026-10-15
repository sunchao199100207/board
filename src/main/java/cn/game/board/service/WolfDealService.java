package cn.game.board.service;

import cn.game.board.model.werewolf.WolfCardView;
import cn.game.board.model.werewolf.WolfClientMessage;
import cn.game.board.model.werewolf.WolfPlayer;
import cn.game.board.model.werewolf.WolfPush;
import cn.game.board.model.werewolf.WolfRole;
import cn.game.board.model.werewolf.WolfRoleConfig;
import cn.game.board.model.werewolf.WolfRoom;
import cn.game.board.model.werewolf.WolfRoomView;
import cn.game.board.model.werewolf.WolfSeatView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 单身份狼人杀发牌。大家都用房间号进来，法官坐最上方位，号码在下面。
 * 法官看得到整副牌，玩家只看自己的一张。夜里流程仍由法官口述。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WolfDealService {

    static final String TOPIC_PREFIX = "/topic/wolf/player/";
    private static final Pattern ROOM_NUMBER = Pattern.compile("\\d{4,6}");
    private static final Pattern PLAYER_ID = Pattern.compile("[A-Za-z0-9\\-]{8,64}");

    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, WolfRoom> rooms = new ConcurrentHashMap<>();
    private final Map<String, String> playerRoomIndex = new ConcurrentHashMap<>();

    public void enter(WolfClientMessage msg) {
        String playerId = requirePlayerId(msg);
        String roomNumber = requireRoomNumber(msg.getRoomNumber());
        departOtherRooms(playerId, roomNumber);
        WolfRoom room = rooms.computeIfAbsent(roomNumber, key -> {
            WolfRoom created = new WolfRoom(roomNumber);
            created.setConfig(WolfDeck.defaultConfig());
            log.info("狼人发牌房间 {} 已创建", roomNumber);
            return created;
        });
        synchronized (room) {
            if (playerId.equals(room.getHostPlayerId())) {
                bind(playerId, roomNumber);
            } else {
                WolfPlayer player = room.getPlayers().get(playerId);
                if (player == null) {
                    if (room.isDealt()) {
                        throw new IllegalStateException("已经发牌，不能再加入");
                    }
                    boolean judgeOpen = room.getHostPlayerId() == null;
                    if (room.getPlayers().size() >= totalOf(room) && !judgeOpen) {
                        throw new IllegalStateException("房间已满");
                    }
                    player = new WolfPlayer();
                    player.setPlayerId(playerId);
                    room.getPlayers().put(playerId, player);
                    log.info("玩家 {} 进入狼人房间 {}", playerId, roomNumber);
                }
                bind(playerId, roomNumber);
            }
        }
        publish(room);
    }

    public void claimJudge(WolfClientMessage msg) {
        String playerId = requirePlayerId(msg);
        WolfRoom room = requireRoomOf(playerId);
        synchronized (room) {
            if (room.isDealt()) {
                throw new IllegalStateException("已经发牌，不能再换法官");
            }
            if (playerId.equals(room.getHostPlayerId())) {
                publish(room);
                return;
            }
            String previousHost = room.getHostPlayerId();
            room.getPlayers().remove(playerId);
            if (previousHost != null) {
                WolfPlayer previous = new WolfPlayer();
                previous.setPlayerId(previousHost);
                room.getPlayers().put(previousHost, previous);
            }
            room.setHostPlayerId(playerId);
            log.info("玩家 {} 接任房间 {} 的法官，原法官 {}", playerId, room.getRoomNumber(), previousHost);
        }
        publish(room);
    }

    public void updateConfig(WolfClientMessage msg) {
        String playerId = requirePlayerId(msg);
        WolfRoom room = requireRoomOf(playerId);
        synchronized (room) {
            if (!playerId.equals(room.getHostPlayerId())) {
                throw new IllegalStateException("只有法官可以配牌");
            }
            if (room.isDealt()) {
                throw new IllegalStateException("已经发牌，不能改配置");
            }
            int total = WolfDeck.build(msg.getConfig()).size();
            room.setConfig(copyConfig(msg.getConfig()));
            for (WolfPlayer player : room.getPlayers().values()) {
                if (player.getSeatNumber() > total) {
                    player.setSeatNumber(0);
                    player.setReady(false);
                }
            }
        }
        publish(room);
    }

    public void sync(WolfClientMessage msg) {
        String playerId = requirePlayerId(msg);
        String roomNumber = playerRoomIndex.get(playerId);
        if (roomNumber == null) {
            throw new IllegalStateException("你不在房间里");
        }
        publish(requireRoom(roomNumber));
    }

    public void selectSeat(WolfClientMessage msg) {
        String playerId = requirePlayerId(msg);
        if (msg.getSeatNumber() == null) {
            throw new IllegalArgumentException("请选择座位");
        }
        WolfRoom room = requireRoomOf(playerId);
        synchronized (room) {
            if (room.isDealt()) {
                throw new IllegalStateException("已经发牌，不能换座");
            }
            int seat = msg.getSeatNumber();
            int total = totalOf(room);
            if (seat == 0) {
                WolfPlayer sitting = room.getPlayers().get(playerId);
                if (sitting != null) {
                    sitting.setSeatNumber(0);
                    sitting.setReady(false);
                }
                publish(room);
                return;
            }
            if (seat < 1 || seat > total) {
                throw new IllegalArgumentException("座位号无效");
            }
            boolean taken = room.getPlayers().values().stream()
                    .anyMatch(other -> other.getSeatNumber() == seat && !other.getPlayerId().equals(playerId));
            if (taken) {
                throw new IllegalStateException("这个座位已经有人了");
            }
            WolfPlayer player = room.getPlayers().get(playerId);
            if (playerId.equals(room.getHostPlayerId())) {
                room.setHostPlayerId(null);
                player = new WolfPlayer();
                player.setPlayerId(playerId);
                room.getPlayers().put(playerId, player);
            }
            if (player == null) {
                throw new IllegalStateException("你不在这个房间里");
            }
            if (player.getSeatNumber() == seat) {
                publish(room);
                return;
            }
            player.setSeatNumber(seat);
            player.setReady(false);
        }
        publish(room);
    }

    public void setReady(WolfClientMessage msg) {
        String playerId = requirePlayerId(msg);
        WolfRoom room = requireRoomOf(playerId);
        synchronized (room) {
            WolfPlayer player = requirePlayer(room, playerId);
            if (room.isDealt()) {
                throw new IllegalStateException("已经发牌");
            }
            if (player.getSeatNumber() <= 0) {
                throw new IllegalStateException("请先选座");
            }
            boolean ready = msg.getReady() == null || msg.getReady();
            player.setReady(ready);
        }
        publish(room);
    }

    public void deal(WolfClientMessage msg) {
        String playerId = requirePlayerId(msg);
        WolfRoom room = requireRoomOf(playerId);
        synchronized (room) {
            if (!playerId.equals(room.getHostPlayerId())) {
                throw new IllegalStateException("只有法官可以发牌");
            }
            if (room.isDealt()) {
                throw new IllegalStateException("这一局已经发过牌");
            }
            List<String> deck = WolfDeck.shuffle(WolfDeck.build(room.getConfig()), new Random());
            List<WolfPlayer> seated = seatedReadyPlayers(room, deck.size());
            for (int i = 0; i < seated.size(); i++) {
                seated.get(i).setRole(deck.get(i));
            }
            room.setDealt(true);
            log.info("房间 {} 第 {} 局已发牌，共 {} 张", room.getRoomNumber(), room.getRound(), deck.size());
        }
        publish(room);
    }

    public void newRound(WolfClientMessage msg) {
        String playerId = requirePlayerId(msg);
        WolfRoom room = requireRoomOf(playerId);
        synchronized (room) {
            if (!playerId.equals(room.getHostPlayerId())) {
                throw new IllegalStateException("只有法官可以再发一局");
            }
            if (!room.isDealt()) {
                throw new IllegalStateException("还没发牌");
            }
            room.setDealt(false);
            room.setRound(room.getRound() + 1);
            for (WolfPlayer player : room.getPlayers().values()) {
                player.setRole(null);
                player.setReady(false);
            }
            log.info("房间 {} 进入第 {} 局，等待重新准备", room.getRoomNumber(), room.getRound());
        }
        publish(room);
    }

    public void leave(WolfClientMessage msg) {
        String playerId = requirePlayerId(msg);
        String roomNumber = playerRoomIndex.get(playerId);
        if (roomNumber == null) {
            sendError(playerId, "你不在房间里");
            return;
        }
        WolfRoom room = rooms.get(roomNumber);
        if (room == null) {
            playerRoomIndex.remove(playerId);
            sendError(playerId, "房间已不存在");
            return;
        }
        synchronized (room) {
            playerRoomIndex.remove(playerId);
            if (!room.isDealt()) {
                if (playerId.equals(room.getHostPlayerId())) {
                    room.setHostPlayerId(null);
                } else {
                    room.getPlayers().remove(playerId);
                }
            }
        }
        sendClosed(playerId, "你已离开房间");
        publish(room);
    }

    public void close(WolfClientMessage msg) {
        String playerId = requirePlayerId(msg);
        WolfRoom room = requireRoomOf(playerId);
        List<String> listeners;
        synchronized (room) {
            if (!playerId.equals(room.getHostPlayerId())) {
                throw new IllegalStateException("只有法官可以解散房间");
            }
            listeners = new ArrayList<>();
            listeners.add(room.getHostPlayerId());
            listeners.addAll(room.getPlayers().keySet());
            rooms.remove(room.getRoomNumber());
            listeners.forEach(playerRoomIndex::remove);
        }
        log.info("房间 {} 已解散", room.getRoomNumber());
        listeners.forEach(id -> sendClosed(id, "房间已解散"));
    }

    public void sendError(String playerId, String message) {
        if (playerId == null || playerId.isBlank()) {
            return;
        }
        messagingTemplate.convertAndSend(topic(playerId), WolfPush.builder().type("error").error(message).build());
    }

    private void publish(WolfRoom room) {
        synchronized (room) {
            publishLocked(room);
        }
    }

    private void publishLocked(WolfRoom room) {
        int total = totalOf(room);
        if (room.getHostPlayerId() != null && room.getRoomNumber().equals(playerRoomIndex.get(room.getHostPlayerId()))) {
            messagingTemplate.convertAndSend(topic(room.getHostPlayerId()), statePush(buildView(room, null, true, total)));
        }
        for (WolfPlayer player : room.getPlayers().values()) {
            if (!room.getRoomNumber().equals(playerRoomIndex.get(player.getPlayerId()))) {
                continue;
            }
            messagingTemplate.convertAndSend(topic(player.getPlayerId()), statePush(buildView(room, player, false, total)));
        }
    }

    private WolfPush statePush(WolfRoomView view) {
        return WolfPush.builder().type("state").state(view).build();
    }

    private void sendClosed(String playerId, String message) {
        messagingTemplate.convertAndSend(topic(playerId), WolfPush.builder().type("closed").error(message).build());
    }

    WolfRoomView buildView(WolfRoom room, WolfPlayer self, boolean host, int total) {
        Map<Integer, WolfPlayer> bySeat = new HashMap<>();
        int readyCount = 0;
        for (WolfPlayer player : room.getPlayers().values()) {
            if (player.getSeatNumber() > 0) {
                bySeat.put(player.getSeatNumber(), player);
                if (player.isReady()) {
                    readyCount++;
                }
            }
        }
        List<WolfSeatView> seats = new ArrayList<>(total);
        for (int seat = 1; seat <= total; seat++) {
            WolfPlayer occupier = bySeat.get(seat);
            WolfSeatView.WolfSeatViewBuilder seatView = WolfSeatView.builder()
                    .seat(seat)
                    .occupied(occupier != null)
                    .ready(occupier != null && occupier.isReady())
                    .self(self != null && occupier == self);
            boolean reveal = room.isDealt() && occupier != null && occupier.getRole() != null
                    && (host || occupier == self);
            if (reveal) {
                WolfRole role = WolfRole.fromKey(occupier.getRole());
                seatView.roleName(role.getName()).emoji(role.getEmoji()).camp(role.getCamp());
            }
            seats.add(seatView.build());
        }
        boolean canDeal = host && !room.isDealt() && readyCount == total && bySeat.size() == total;
        WolfCardView card = null;
        if (!host && room.isDealt() && self != null && self.getRole() != null) {
            card = WolfDeck.toCard(self.getRole());
        }
        return WolfRoomView.builder()
                .roomNumber(room.getRoomNumber())
                .host(host)
                .judgeTaken(room.getHostPlayerId() != null)
                .playerId(host ? room.getHostPlayerId() : self.getPlayerId())
                .config(copyConfig(room.getConfig()))
                .total(total)
                .round(room.getRound())
                .dealt(room.isDealt())
                .canDeal(canDeal)
                .seated(bySeat.size())
                .readyCount(readyCount)
                .mySeat(self == null || self.getSeatNumber() <= 0 ? null : self.getSeatNumber())
                .ready(self != null && self.isReady())
                .configText(WolfDeck.describe(room.getConfig()))
                .seats(seats)
                .script(host ? WolfDeck.script(room.getConfig()) : List.of())
                .card(card)
                .build();
    }

    private List<WolfPlayer> seatedReadyPlayers(WolfRoom room, int total) {
        Map<Integer, WolfPlayer> bySeat = new HashMap<>();
        for (WolfPlayer player : room.getPlayers().values()) {
            if (player.getSeatNumber() > 0) {
                bySeat.put(player.getSeatNumber(), player);
            }
        }
        if (bySeat.size() != total) {
            throw new IllegalStateException("还有空位，不能发牌");
        }
        List<WolfPlayer> seated = new ArrayList<>(bySeat.values());
        seated.sort(Comparator.comparingInt(WolfPlayer::getSeatNumber));
        for (int seat = 1; seat <= total; seat++) {
            WolfPlayer player = bySeat.get(seat);
            if (player == null || !player.isReady()) {
                throw new IllegalStateException("还有玩家未入座或未准备");
            }
        }
        return seated;
    }

    private WolfRoom requireRoomOf(String playerId) {
        String roomNumber = playerRoomIndex.get(playerId);
        if (roomNumber == null) {
            throw new IllegalStateException("你不在房间里");
        }
        return requireRoom(roomNumber);
    }

    private WolfRoom requireRoom(String roomNumber) {
        String normalized = requireRoomNumber(roomNumber);
        WolfRoom room = rooms.get(normalized);
        if (room == null) {
            throw new IllegalArgumentException("房间不存在");
        }
        return room;
    }

    private WolfPlayer requirePlayer(WolfRoom room, String playerId) {
        if (playerId.equals(room.getHostPlayerId())) {
            throw new IllegalStateException("法官不入座");
        }
        WolfPlayer player = room.getPlayers().get(playerId);
        if (player == null) {
            throw new IllegalStateException("你不在这个房间里");
        }
        return player;
    }

    /** 进另一个房间号时，先彻底离开原来的房间，再进新的。同一房间号则保留座位和身份。 */
    private void departOtherRooms(String playerId, String targetRoom) {
        List<WolfRoom> left = new ArrayList<>();
        for (WolfRoom other : rooms.values()) {
            if (other.getRoomNumber().equals(targetRoom)) {
                continue;
            }
            synchronized (other) {
                boolean host = playerId.equals(other.getHostPlayerId());
                boolean seated = other.getPlayers().containsKey(playerId);
                if (!host && !seated) {
                    continue;
                }
                if (host) {
                    other.setHostPlayerId(null);
                }
                other.getPlayers().remove(playerId);
                if (other.getRoomNumber().equals(playerRoomIndex.get(playerId))) {
                    playerRoomIndex.remove(playerId);
                }
                left.add(other);
                log.info("玩家 {} 离开房间 {}，改入 {}", playerId, other.getRoomNumber(), targetRoom);
            }
        }
        String indexed = playerRoomIndex.get(playerId);
        if (indexed != null && !indexed.equals(targetRoom)) {
            playerRoomIndex.remove(playerId);
        }
        for (WolfRoom other : left) {
            publish(other);
        }
    }

    private void bind(String playerId, String roomNumber) {
        playerRoomIndex.put(playerId, roomNumber);
    }

    private int totalOf(WolfRoom room) {
        return WolfDeck.build(room.getConfig()).size();
    }

    private String requirePlayerId(WolfClientMessage msg) {
        if (msg == null || msg.getPlayerId() == null || !PLAYER_ID.matcher(msg.getPlayerId()).matches()) {
            throw new IllegalArgumentException("玩家标识无效");
        }
        return msg.getPlayerId();
    }

    private String requireRoomNumber(String roomNumber) {
        if (roomNumber == null || !ROOM_NUMBER.matcher(roomNumber.trim()).matches()) {
            throw new IllegalArgumentException("房间号需要 4 到 6 位数字");
        }
        return roomNumber.trim();
    }

    private WolfRoleConfig copyConfig(WolfRoleConfig source) {
        WolfRoleConfig copy = new WolfRoleConfig();
        copy.setCivilian(source.getCivilian());
        copy.setWerewolf(source.getWerewolf());
        copy.setWolfKing(source.isWolfKing());
        copy.setWhiteWolfKing(source.isWhiteWolfKing());
        copy.setSeer(source.isSeer());
        copy.setWitch(source.isWitch());
        copy.setHunter(source.isHunter());
        copy.setGuard(source.isGuard());
        copy.setThief(source.isThief());
        copy.setKnight(source.isKnight());
        copy.setSilenceElder(source.isSilenceElder());
        return copy;
    }

    private String topic(String playerId) {
        return TOPIC_PREFIX + playerId;
    }
}
