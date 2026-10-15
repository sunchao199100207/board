package cn.game.board.model.dual;

import lombok.Data;

@Data
public class DualPlayer {
    private String playerId;
    private int seatNumber;
    private boolean ready;
    private String cardA;
    private String cardB;
    /** 确认后的上身份：0 是 cardA，1 是 cardB。 */
    private Integer upperIndex;
}
