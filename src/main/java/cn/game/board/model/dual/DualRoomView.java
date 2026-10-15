package cn.game.board.model.dual;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DualRoomView {
    private String roomNumber;
    private boolean host;
    private boolean judgeTaken;
    private String playerId;
    private DualConfig config;
    private int total;
    private int round;
    private boolean dealt;
    private boolean canDeal;
    private int seated;
    private int readyCount;
    private int confirmedCount;
    private Integer mySeat;
    private boolean ready;
    private String configText;
    private List<DualSeatView> seats;
    private DualHandView hand;
}
