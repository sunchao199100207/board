package cn.game.board.service;

import cn.game.board.model.werewolf.WolfClientMessage;
import cn.game.board.model.werewolf.WolfPush;
import cn.game.board.model.werewolf.WolfRoleConfig;
import cn.game.board.model.werewolf.WolfRoomView;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
class WolfDealServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private WolfDealService service;
    private final Map<String, WolfPush> inbox = new HashMap<>();

    @BeforeEach
    void setUp() {
        service = new WolfDealService(messagingTemplate);
        doAnswer(invocation -> {
            inbox.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(messagingTemplate).convertAndSend(anyString(), any(WolfPush.class));
    }

    @Test
    void dealGivesEachPlayerOnlyTheirOwnCard() throws Exception {
        sitJudge("host-0001", "6666", null);

        List<String> players = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            String playerId = "player-" + String.format("%02d", i);
            players.add(playerId);
            service.enter(message(playerId, "6666", null));
            WolfClientMessage seat = message(playerId, "6666", null);
            seat.setSeatNumber(i);
            service.selectSeat(seat);
            WolfClientMessage ready = message(playerId, "6666", null);
            ready.setReady(true);
            service.setReady(ready);
        }
        service.deal(message("host-0001", "6666", null));

        WolfRoomView hostView = inbox.get(WolfDealService.TOPIC_PREFIX + "host-0001").getState();
        assertTrue(hostView.isHost());
        assertTrue(hostView.isDealt());
        assertNull(hostView.getCard());
        assertFalse(hostView.getScript().isEmpty());
        assertEquals(10, hostView.getSeats().stream().filter(seat -> seat.getRoleName() != null).count());
        String hostJson = new ObjectMapper().writeValueAsString(hostView);
        assertFalse(hostJson.contains("\"role\""));

        Map<String, Integer> dealt = new HashMap<>();
        for (String playerId : players) {
            WolfRoomView view = inbox.get(WolfDealService.TOPIC_PREFIX + playerId).getState();
            assertFalse(view.isHost());
            assertTrue(view.getScript().isEmpty());
            assertNotNull(view.getCard());
            assertEquals(playerId, view.getPlayerId());
            assertEquals(1, view.getSeats().stream().filter(seat -> seat.getRoleName() != null).count());
            dealt.merge(view.getCard().getRole(), 1, Integer::sum);
            String json = new ObjectMapper().writeValueAsString(view);
            for (String other : List.of("civilian", "werewolf", "wolfKing", "whiteWolfKing", "seer", "witch", "hunter", "guard", "thief", "knight", "silenceElder")) {
                if (!other.equals(view.getCard().getRole())) {
                    assertFalse(json.contains("\"role\":\"" + other + "\""));
                }
            }
        }
        assertEquals(3, dealt.get("civilian"));
        assertEquals(2, dealt.get("werewolf"));
        assertEquals(1, dealt.get("wolfKing"));
        assertEquals(1, dealt.get("seer"));
        assertEquals(1, dealt.get("witch"));
        assertEquals(1, dealt.get("hunter"));
        assertEquals(1, dealt.get("guard"));
    }

    @Test
    void rejectsWolfMajorityAndDuplicateSeat() {
        WolfRoleConfig tooManyWolves = new WolfRoleConfig();
        tooManyWolves.setCivilian(3);
        tooManyWolves.setWerewolf(3);
        assertThrows(IllegalArgumentException.class, () -> WolfDeck.build(tooManyWolves));

        sitJudge("host-0001", "6666", smallSix());
        service.enter(message("player-01", "6666", null));
        service.enter(message("player-02", "6666", null));
        WolfClientMessage first = message("player-01", "6666", null);
        first.setSeatNumber(1);
        service.selectSeat(first);
        WolfClientMessage second = message("player-02", "6666", null);
        second.setSeatNumber(1);
        assertThrows(IllegalStateException.class, () -> service.selectSeat(second));
    }

    @Test
    void judgeSeatCanBeTakenOver() {
        sitJudge("host-0001", "6666", null);
        service.enter(message("player-01", "6666", null));
        service.claimJudge(message("player-01", "6666", null));

        WolfRoomView previous = inbox.get(WolfDealService.TOPIC_PREFIX + "host-0001").getState();
        WolfRoomView current = inbox.get(WolfDealService.TOPIC_PREFIX + "player-01").getState();
        assertFalse(previous.isHost());
        assertTrue(current.isHost());
        assertNull(previous.getMySeat());

        WolfRoleConfig config = smallSix();
        config.setWhiteWolfKing(true);
        service.updateConfig(message("player-01", "6666", config));
        assertTrue(inbox.get(WolfDealService.TOPIC_PREFIX + "player-01").getState().getConfig().isWhiteWolfKing());
        assertThrows(IllegalStateException.class, () -> service.updateConfig(message("host-0001", "6666", smallSix())));
    }

    @Test
    void playerCanMoveAndLeaveSeat() {
        sitJudge("host-0001", "6666", smallSix());
        service.enter(message("player-01", "6666", null));
        WolfClientMessage first = message("player-01", "6666", null);
        first.setSeatNumber(1);
        service.selectSeat(first);
        WolfClientMessage ready = message("player-01", "6666", null);
        ready.setReady(true);
        service.setReady(ready);

        WolfClientMessage move = message("player-01", "6666", null);
        move.setSeatNumber(3);
        service.selectSeat(move);
        WolfRoomView moved = inbox.get(WolfDealService.TOPIC_PREFIX + "player-01").getState();
        assertEquals(3, moved.getMySeat());
        assertFalse(moved.isReady());

        WolfClientMessage stand = message("player-01", "6666", null);
        stand.setSeatNumber(0);
        service.selectSeat(stand);
        WolfRoomView standing = inbox.get(WolfDealService.TOPIC_PREFIX + "player-01").getState();
        assertNull(standing.getMySeat());
    }

    @Test
    void enterAnotherRoomLeavesThePreviousOne() {
        service.enter(message("player-01", "1233", null));
        WolfClientMessage seat = message("player-01", "1233", null);
        seat.setSeatNumber(1);
        service.selectSeat(seat);

        service.enter(message("player-01", "1234", null));

        WolfRoomView current = inbox.get(WolfDealService.TOPIC_PREFIX + "player-01").getState();
        assertEquals("1234", current.getRoomNumber());
        assertNull(current.getMySeat());

        service.enter(message("player-02", "1233", null));
        WolfRoomView previous = inbox.get(WolfDealService.TOPIC_PREFIX + "player-02").getState();
        assertEquals(0, previous.getSeated());
    }

    @Test
    void enterAnotherRoomAfterDealDropsTheOldSeat() {
        sitJudge("host-0001", "1233", smallSix());
        for (int i = 1; i <= 6; i++) {
            String playerId = "player-" + String.format("%02d", i);
            service.enter(message(playerId, "1233", null));
            WolfClientMessage seat = message(playerId, "1233", null);
            seat.setSeatNumber(i);
            service.selectSeat(seat);
            WolfClientMessage ready = message(playerId, "1233", null);
            ready.setReady(true);
            service.setReady(ready);
        }
        service.deal(message("host-0001", "1233", null));

        service.enter(message("player-01", "1234", null));
        WolfRoomView moved = inbox.get(WolfDealService.TOPIC_PREFIX + "player-01").getState();
        assertEquals("1234", moved.getRoomNumber());
        assertNull(moved.getCard());

        assertThrows(IllegalStateException.class, () -> service.enter(message("player-01", "1233", null)));
    }

    @Test
    void reenterAfterDealShowsTheSameCard() {
        sitJudge("host-0001", "6666", smallSix());
        service.enter(message("player-01", "6666", null));
        WolfClientMessage seat = message("player-01", "6666", null);
        seat.setSeatNumber(1);
        service.selectSeat(seat);
        WolfClientMessage ready = message("player-01", "6666", null);
        ready.setReady(true);
        service.setReady(ready);
        for (int i = 2; i <= 6; i++) {
            String playerId = "player-" + String.format("%02d", i);
            service.enter(message(playerId, "6666", null));
            WolfClientMessage otherSeat = message(playerId, "6666", null);
            otherSeat.setSeatNumber(i);
            service.selectSeat(otherSeat);
            WolfClientMessage otherReady = message(playerId, "6666", null);
            otherReady.setReady(true);
            service.setReady(otherReady);
        }
        service.deal(message("host-0001", "6666", null));
        WolfRoomView dealt = inbox.get(WolfDealService.TOPIC_PREFIX + "player-01").getState();

        service.enter(message("player-01", "6666", null));
        service.enter(message("host-0001", "6666", null));

        WolfRoomView again = inbox.get(WolfDealService.TOPIC_PREFIX + "player-01").getState();
        WolfRoomView host = inbox.get(WolfDealService.TOPIC_PREFIX + "host-0001").getState();
        assertEquals(1, again.getMySeat());
        assertEquals(dealt.getCard().getRole(), again.getCard().getRole());
        assertTrue(host.isHost());
        assertTrue(host.isDealt());
        assertEquals(6, host.getSeats().stream().filter(item -> item.getRoleName() != null).count());
    }

    @Test
    void leaveAfterDealThenEnterShowsTheSameCard() {
        sitJudge("host-0001", "6666", smallSix());
        for (int i = 1; i <= 6; i++) {
            String playerId = "player-" + String.format("%02d", i);
            service.enter(message(playerId, "6666", null));
            WolfClientMessage seat = message(playerId, "6666", null);
            seat.setSeatNumber(i);
            service.selectSeat(seat);
            WolfClientMessage ready = message(playerId, "6666", null);
            ready.setReady(true);
            service.setReady(ready);
        }
        service.deal(message("host-0001", "6666", null));
        String role = inbox.get(WolfDealService.TOPIC_PREFIX + "player-01").getState().getCard().getRole();

        service.leave(message("player-01", "6666", null));
        service.leave(message("host-0001", "6666", null));
        service.enter(message("player-01", "6666", null));
        service.enter(message("host-0001", "6666", null));

        WolfRoomView player = inbox.get(WolfDealService.TOPIC_PREFIX + "player-01").getState();
        WolfRoomView host = inbox.get(WolfDealService.TOPIC_PREFIX + "host-0001").getState();
        assertEquals(1, player.getMySeat());
        assertEquals(role, player.getCard().getRole());
        assertTrue(host.isHost());
        assertEquals(6, host.getSeats().stream().filter(seat -> seat.getRoleName() != null).count());
    }

    @Test
    void newRoundClearsCards() {
        sitJudge("host-0001", "6666", smallSix());
        for (int i = 1; i <= 6; i++) {
            String playerId = "player-" + String.format("%02d", i);
            service.enter(message(playerId, "6666", null));
            WolfClientMessage seat = message(playerId, "6666", null);
            seat.setSeatNumber(i);
            service.selectSeat(seat);
            WolfClientMessage ready = message(playerId, "6666", null);
            ready.setReady(true);
            service.setReady(ready);
        }
        service.deal(message("host-0001", "6666", null));
        service.newRound(message("host-0001", "6666", null));

        WolfRoomView player = inbox.get(WolfDealService.TOPIC_PREFIX + "player-01").getState();
        assertFalse(player.isDealt());
        assertNull(player.getCard());
        assertEquals(2, player.getRound());
        assertFalse(player.isReady());
    }

    private void sitJudge(String hostId, String roomNumber, WolfRoleConfig config) {
        service.enter(message(hostId, roomNumber, null));
        service.claimJudge(message(hostId, roomNumber, null));
        if (config != null) {
            service.updateConfig(message(hostId, roomNumber, config));
        }
    }

    private WolfClientMessage message(String playerId, String roomNumber, WolfRoleConfig config) {
        WolfClientMessage message = new WolfClientMessage();
        message.setPlayerId(playerId);
        message.setRoomNumber(roomNumber);
        message.setConfig(config);
        return message;
    }

    private WolfRoleConfig standardTen() {
        WolfRoleConfig config = new WolfRoleConfig();
        config.setCivilian(3);
        config.setWerewolf(2);
        config.setWolfKing(true);
        config.setSeer(true);
        config.setWitch(true);
        config.setHunter(true);
        config.setGuard(true);
        return config;
    }

    private WolfRoleConfig smallSix() {
        WolfRoleConfig config = new WolfRoleConfig();
        config.setCivilian(3);
        config.setWerewolf(2);
        config.setSeer(true);
        return config;
    }
}
