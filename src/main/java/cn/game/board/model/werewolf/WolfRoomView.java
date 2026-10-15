package cn.game.board.model.werewolf;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 推给某一个连接的房间视图。card 只在该玩家已发到牌时出现。
 */
@Data
@Builder
public class WolfRoomView {
    private String roomNumber;
    private boolean host;
    private boolean judgeTaken;
    private String playerId;
    private WolfRoleConfig config;
    private int total;
    private int round;
    private boolean dealt;
    private boolean canDeal;
    private int seated;
    private int readyCount;
    private Integer mySeat;
    private boolean ready;
    private String configText;
    private List<WolfSeatView> seats;
    private List<WolfScriptStep> script;
    private WolfCardView card;
}
