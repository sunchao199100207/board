package cn.game.board.service;

import cn.game.board.model.dual.DualCardView;
import cn.game.board.model.dual.DualClientMessage;
import cn.game.board.model.dual.DualConfig;
import cn.game.board.model.dual.DualHandView;
import cn.game.board.model.dual.DualPlayer;
import cn.game.board.model.dual.DualPush;
import cn.game.board.model.dual.DualRole;
import cn.game.board.model.dual.DualRoom;
import cn.game.board.model.dual.DualRoomView;
import cn.game.board.model.dual.DualSeatView;
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
 * 双身份狼人杀发牌。每人两张牌，玩家自己选定上身份。夜里仍由法官口述。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DualDealService {

    static final String TOPIC_PREFIX = "/topic/dual/player/";
    private static final Pattern ROOM_NUMBER = Pattern.compile("\\d{4,6}");
    private static final Pattern PLAYER_ID = Pattern.compile("[A-Za-z0-9\\-]{8,64}");

    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, DualRoom> rooms = new ConcurrentHashMap<>();
    private final Map<String, String> playerRoomIndex = new ConcurrentHashMap<>();

    public void enter(DualClientMessage msg) {
        String playerId = requirePlayerId(msg);
        String roomNumber = requireRoomNumber(msg.getRoomNumber());
        departOtherRooms(playerId, roomNumber);
        DualRoom room = rooms.computeIfAbsent(roomNumber, key -> {
            DualRoom created = new DualRoom(roomNumber);
            created.setConfig(DualDeck.preset(6));
            log.info("双身份房间 {} 已创建", roomNumber);
            return created;
        });
        synchronized (room) {
            if (playerId.equals(room.getHostPlayerId())) {
                bind(playerId, roomNumber);
            } else {
                DualPlayer player = room.getPlayers().get(playerId);
                if (player == null) {
                    if (room.isDealt()) {
                        throw new IllegalStateException("已经发牌，不能再加入");
                    }
                    boolean judgeOpen = room.getHostPlayerId() == null;
                    if (room.getPlayers().size() >= room.getConfig().getPlayers() && !judgeOpen) {
                        throw new IllegalStateException("房间已满");
                    }
                    player = new DualPlayer();
                    player.setPlayerId(playerId);
                    room.getPlayers().put(playerId, player);
                    log.info("玩家 {} 进入双身份房间 {}", playerId, roomNumber);
                }
                bind(playerId, roomNumber);
            }
        }
        publish(room);
    }

    public void claimJudge(DualClientMessage msg) {
        String playerId = requirePlayerId(msg);
        DualRoom room = requireRoomOf(playerId);
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
                DualPlayer previous = new DualPlayer();
                previous.setPlayerId(previousHost);
                room.getPlayers().put(previousHost, previous);
            }
            room.setHostPlayerId(playerId);
        }
        publish(room);
    }

    public void updateConfig(DualClientMessage msg) {
        String playerId = requirePlayerId(msg);
        DualRoom room = requireRoomOf(playerId);
        synchronized (room) {
            if (!playerId.equals(room.getHostPlayerId())) {
                throw new IllegalStateException("只有法官可以改人数");
            }
            if (room.isDealt()) {
                throw new IllegalStateException("已经发牌，不能改配置");
            }
            DualConfig config = readConfig(msg, room.getConfig());
            DualDeck.pool(config);
            room.setConfig(config);
            for (DualPlayer player : room.getPlayers().values()) {
                if (player.getSeatNumber() > config.getPlayers()) {
                    player.setSeatNumber(0);
                    player.setReady(false);
                }
            }
        }
        publish(room);
    }

    public void selectSeat(DualClientMessage msg) {
        String playerId = requirePlayerId(msg);
        if (msg.getSeatNumber() == null) {
            throw new IllegalArgumentException("请选择座位");
        }
        DualRoom room = requireRoomOf(playerId);
        synchronized (room) {
            if (room.isDealt()) {
                throw new IllegalStateException("已经发牌，不能换座");
            }
            int seat = msg.getSeatNumber();
            int total = room.getConfig().getPlayers();
            if (seat == 0) {
                DualPlayer sitting = room.getPlayers().get(playerId);
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
            DualPlayer player = room.getPlayers().get(playerId);
            if (playerId.equals(room.getHostPlayerId())) {
                room.setHostPlayerId(null);
                player = new DualPlayer();
                player.setPlayerId(playerId);
                room.getPlayers().put(playerId, player);
            }
            if (player == null) {
                throw new IllegalStateException("你不在这个房间里");
            }
            player.setSeatNumber(seat);
            player.setReady(false);
        }
        publish(room);
    }

    public void setReady(DualClientMessage msg) {
        String playerId = requirePlayerId(msg);
        DualRoom room = requireRoomOf(playerId);
        synchronized (room) {
            if (playerId.equals(room.getHostPlayerId())) {
                throw new IllegalStateException("法官不用准备");
            }
            DualPlayer player = room.getPlayers().get(playerId);
            if (player == null || player.getSeatNumber() <= 0) {
                throw new IllegalStateException("请先选座");
            }
            if (room.isDealt()) {
                throw new IllegalStateException("已经发牌");
            }
            player.setReady(msg.getReady() == null || msg.getReady());
        }
        publish(room);
    }

    public void deal(DualClientMessage msg) {
        String playerId = requirePlayerId(msg);
        DualRoom room = requireRoomOf(playerId);
        synchronized (room) {
            if (!playerId.equals(room.getHostPlayerId())) {
                throw new IllegalStateException("只有法官可以发牌");
            }
            if (room.isDealt()) {
                throw new IllegalStateException("这一局已经发过牌");
            }
            List<DualPlayer> seated = seatedReady(room);
            List<String[]> pairs = DualDeck.deal(room.getConfig(), new Random());
            if (pairs.size() != seated.size()) {
                throw new IllegalStateException("牌和人数对不上");
            }
            for (int i = 0; i < seated.size(); i++) {
                seated.get(i).setCardA(pairs.get(i)[0]);
                seated.get(i).setCardB(pairs.get(i)[1]);
                seated.get(i).setUpperIndex(null);
            }
            room.getDeadCards().clear();
            room.setDealt(true);
            log.info("双身份房间 {} 第 {} 局已发牌", room.getRoomNumber(), room.getRound());
        }
        publish(room);
    }

    public void choose(DualClientMessage msg) {
        String playerId = requirePlayerId(msg);
        DualRoom room = requireRoomOf(playerId);
        synchronized (room) {
            if (!room.isDealt()) {
                throw new IllegalStateException("还没发牌");
            }
            DualPlayer player = room.getPlayers().get(playerId);
            if (player == null || player.getCardA() == null) {
                throw new IllegalStateException("你没有牌");
            }
            if (player.getUpperIndex() != null) {
                throw new IllegalStateException("上身份已经确认");
            }
            Integer upper = msg.getUpperIndex();
            if (upper == null || (upper != 0 && upper != 1)) {
                throw new IllegalArgumentException("请选择哪一张是上身份");
            }
            player.setUpperIndex(upper);
        }
        publish(room);
    }

    public void mark(DualClientMessage msg) {
        String playerId = requirePlayerId(msg);
        DualRoom room = requireRoomOf(playerId);
        synchronized (room) {
            if (!playerId.equals(room.getHostPlayerId())) {
                throw new IllegalStateException("只有法官可以标记");
            }
            if (!room.isDealt()) {
                throw new IllegalStateException("还没发牌");
            }
            Integer seat = msg.getSeatNumber();
            String card = msg.getCardKey();
            if (seat == null || seat < 1 || (!"A".equals(card) && !"B".equals(card))) {
                throw new IllegalArgumentException("请选择要标记的牌");
            }
            String key = seat + ":" + card;
            if (!room.getDeadCards().add(key)) {
                room.getDeadCards().remove(key);
            }
        }
        publish(room);
    }

    public void newRound(DualClientMessage msg) {
        String playerId = requirePlayerId(msg);
        DualRoom room = requireRoomOf(playerId);
        synchronized (room) {
            if (!playerId.equals(room.getHostPlayerId())) {
                throw new IllegalStateException("只有法官可以再发一局");
            }
            room.setDealt(false);
            room.setRound(room.getRound() + 1);
            room.getDeadCards().clear();
            for (DualPlayer player : room.getPlayers().values()) {
                player.setCardA(null);
                player.setCardB(null);
                player.setUpperIndex(null);
                player.setReady(false);
            }
        }
        publish(room);
    }

    public void leave(DualClientMessage msg) {
        String playerId = requirePlayerId(msg);
        String roomNumber = playerRoomIndex.get(playerId);
        if (roomNumber == null) {
            sendError(playerId, "你不在房间里");
            return;
        }
        DualRoom room = rooms.get(roomNumber);
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

    public void close(DualClientMessage msg) {
        String playerId = requirePlayerId(msg);
        DualRoom room = requireRoomOf(playerId);
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
        listeners.forEach(id -> sendClosed(id, "房间已解散"));
    }

    public void sendError(String playerId, String message) {
        if (playerId == null || playerId.isBlank()) {
            return;
        }
        messagingTemplate.convertAndSend(topic(playerId), DualPush.builder().type("error").error(message).build());
    }

    private void departOtherRooms(String playerId, String targetRoom) {
        List<DualRoom> left = new ArrayList<>();
        for (DualRoom other : rooms.values()) {
            if (other.getRoomNumber().equals(targetRoom)) {
                continue;
            }
            synchronized (other) {
                boolean host = playerId.equals(other.getHostPlayerId());
                boolean member = other.getPlayers().containsKey(playerId);
                if (!host && !member) {
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
            }
        }
        String indexed = playerRoomIndex.get(playerId);
        if (indexed != null && !indexed.equals(targetRoom)) {
            playerRoomIndex.remove(playerId);
        }
        for (DualRoom other : left) {
            publish(other);
        }
    }

    private List<DualPlayer> seatedReady(DualRoom room) {
        int total = room.getConfig().getPlayers();
        Map<Integer, DualPlayer> bySeat = new HashMap<>();
        for (DualPlayer player : room.getPlayers().values()) {
            if (player.getSeatNumber() > 0) {
                bySeat.put(player.getSeatNumber(), player);
            }
        }
        if (bySeat.size() != total) {
            throw new IllegalStateException("还有空位，不能发牌");
        }
        List<DualPlayer> seated = new ArrayList<>();
        for (int seat = 1; seat <= total; seat++) {
            DualPlayer player = bySeat.get(seat);
            if (player == null || !player.isReady()) {
                throw new IllegalStateException("还有玩家未入座或未准备");
            }
            seated.add(player);
        }
        seated.sort(Comparator.comparingInt(DualPlayer::getSeatNumber));
        return seated;
    }

    private void publish(DualRoom room) {
        synchronized (room) {
            int total = room.getConfig().getPlayers();
            if (room.getHostPlayerId() != null && room.getRoomNumber().equals(playerRoomIndex.get(room.getHostPlayerId()))) {
                messagingTemplate.convertAndSend(topic(room.getHostPlayerId()), statePush(buildView(room, null, true, total)));
            }
            for (DualPlayer player : room.getPlayers().values()) {
                if (!room.getRoomNumber().equals(playerRoomIndex.get(player.getPlayerId()))) {
                    continue;
                }
                messagingTemplate.convertAndSend(topic(player.getPlayerId()), statePush(buildView(room, player, false, total)));
            }
        }
    }

    private DualPush statePush(DualRoomView view) {
        return DualPush.builder().type("state").state(view).build();
    }

    private void sendClosed(String playerId, String message) {
        messagingTemplate.convertAndSend(topic(playerId), DualPush.builder().type("closed").error(message).build());
    }

    DualRoomView buildView(DualRoom room, DualPlayer self, boolean host, int total) {
        Map<Integer, DualPlayer> bySeat = new HashMap<>();
        int readyCount = 0;
        int confirmedCount = 0;
        for (DualPlayer player : room.getPlayers().values()) {
            if (player.getUpperIndex() != null) {
                confirmedCount++;
            }
            if (player.getSeatNumber() > 0) {
                bySeat.put(player.getSeatNumber(), player);
                if (player.isReady()) {
                    readyCount++;
                }
            }
        }
        List<DualSeatView> seats = new ArrayList<>();
        for (int seat = 1; seat <= total; seat++) {
            DualPlayer occupier = bySeat.get(seat);
            DualSeatView.DualSeatViewBuilder seatView = DualSeatView.builder()
                    .seat(seat)
                    .occupied(occupier != null)
                    .ready(occupier != null && occupier.isReady())
                    .self(self != null && occupier == self);
            boolean reveal = room.isDealt() && occupier != null && occupier.getCardA() != null && (host || occupier == self);
            if (reveal) {
                DualRole a = DualRole.fromKey(occupier.getCardA());
                DualRole b = DualRole.fromKey(occupier.getCardB());
                seatView.cardAName(a.getName()).cardAEmoji(a.getEmoji())
                        .cardBName(b.getName()).cardBEmoji(b.getEmoji())
                        .upperIndex(occupier.getUpperIndex())
                        .confirmed(occupier.getUpperIndex() != null)
                        .campName(DualDeck.campName(occupier.getCardA(), occupier.getCardB()))
                        .cardADead(room.getDeadCards().contains(seat + ":A"))
                        .cardBDead(room.getDeadCards().contains(seat + ":B"));
            }
            seats.add(seatView.build());
        }
        boolean canDeal = host && !room.isDealt() && readyCount == total && bySeat.size() == total;
        DualHandView hand = null;
        if (!host && room.isDealt() && self != null && self.getCardA() != null) {
            hand = DualHandView.builder()
                    .cardA(DualDeck.toCard(self.getCardA()))
                    .cardB(DualDeck.toCard(self.getCardB()))
                    .upperIndex(self.getUpperIndex())
                    .confirmed(self.getUpperIndex() != null)
                    .campName(DualDeck.campName(self.getCardA(), self.getCardB()))
                    .campTip(DualDeck.campTip(self.getCardA(), self.getCardB()))
                    .golden(DualRole.VILLAGER.getKey().equals(self.getCardA()) && DualRole.VILLAGER.getKey().equals(self.getCardB()))
                    .cardADead(room.getDeadCards().contains(self.getSeatNumber() + ":A"))
                    .cardBDead(room.getDeadCards().contains(self.getSeatNumber() + ":B"))
                    .build();
        }
        return DualRoomView.builder()
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
                .confirmedCount(confirmedCount)
                .mySeat(self == null || self.getSeatNumber() <= 0 ? null : self.getSeatNumber())
                .ready(self != null && self.isReady())
                .configText(DualDeck.describe(room.getConfig()))
                .seats(seats)
                .hand(hand)
                .build();
    }

    private DualConfig readConfig(DualClientMessage msg, DualConfig current) {
        DualConfig config = copyConfig(current);
        if (msg.getPlayers() != null) {
            config.setPlayers(msg.getPlayers());
        }
        return config;
    }

    private DualConfig copyConfig(DualConfig source) {
        DualConfig copy = new DualConfig();
        copy.setPlayers(source.getPlayers());
        return copy;
    }

    private DualRoom requireRoomOf(String playerId) {
        String roomNumber = playerRoomIndex.get(playerId);
        if (roomNumber == null) {
            throw new IllegalStateException("你不在房间里");
        }
        DualRoom room = rooms.get(roomNumber);
        if (room == null) {
            throw new IllegalArgumentException("房间不存在");
        }
        return room;
    }

    private void bind(String playerId, String roomNumber) {
        playerRoomIndex.put(playerId, roomNumber);
    }

    private String topic(String playerId) {
        return TOPIC_PREFIX + playerId;
    }

    private String requirePlayerId(DualClientMessage msg) {
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
}
