package cn.game.board.service;

import cn.game.board.model.dual.DualClientMessage;
import cn.game.board.model.dual.DualPush;
import cn.game.board.model.dual.DualRoomView;
import cn.game.board.model.dual.DualSeatView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.HashMap;
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
class DualDealServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private DualDealService service;
    private final Map<String, DualPush> inbox = new HashMap<>();

    @BeforeEach
    void setUp() {
        service = new DualDealService(messagingTemplate);
        doAnswer(invocation -> {
            inbox.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(messagingTemplate).convertAndSend(anyString(), any(DualPush.class));
    }

    @Test
    void playerSeesOnlyOwnCardsAndChoosesTheUpperOne() {
        sitJudge("host-0001");
        for (int seat = 1; seat <= 6; seat++) {
            String playerId = "player-" + String.format("%02d", seat);
            service.enter(message(playerId));
            DualClientMessage sit = message(playerId);
            sit.setSeatNumber(seat);
            service.selectSeat(sit);
            DualClientMessage ready = message(playerId);
            ready.setReady(true);
            service.setReady(ready);
        }
        service.deal(message("host-0001"));

        DualRoomView self = inbox.get(DualDealService.TOPIC_PREFIX + "player-01").getState();
        assertNotNull(self.getHand());
        assertFalse(self.getHand().isConfirmed());
        long revealed = self.getSeats().stream().filter(seat -> seat.getCardAName() != null).count();
        assertEquals(1, revealed);

        DualRoomView host = inbox.get(DualDealService.TOPIC_PREFIX + "host-0001").getState();
        assertNull(host.getHand());
        assertEquals(6, host.getSeats().stream().filter(seat -> seat.getCardAName() != null).count());

        DualClientMessage choose = message("player-01");
        choose.setUpperIndex(1);
        service.choose(choose);
        DualRoomView chosen = inbox.get(DualDealService.TOPIC_PREFIX + "player-01").getState();
        assertTrue(chosen.getHand().isConfirmed());
        assertEquals(1, chosen.getHand().getUpperIndex());
        DualSeatView hostSeat = inbox.get(DualDealService.TOPIC_PREFIX + "host-0001").getState().getSeats().get(0);
        assertEquals(1, hostSeat.getUpperIndex());
        assertThrows(IllegalStateException.class, () -> service.choose(choose));
    }

    @Test
    void enteringAnotherRoomLeavesThePreviousDualRoom() {
        service.enter(message("player-01", "1233"));
        service.enter(message("player-01", "1234"));
        assertEquals("1234", inbox.get(DualDealService.TOPIC_PREFIX + "player-01").getState().getRoomNumber());
    }

    private void sitJudge(String hostId) {
        service.enter(message(hostId));
        service.claimJudge(message(hostId));
    }

    private DualClientMessage message(String playerId) {
        return message(playerId, "6666");
    }

    private DualClientMessage message(String playerId, String roomNumber) {
        DualClientMessage message = new DualClientMessage();
        message.setPlayerId(playerId);
        message.setRoomNumber(roomNumber);
        return message;
    }
}
