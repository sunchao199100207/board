package cn.game.board.model.dual;

import lombok.Data;

@Data
public class DualClientMessage {
    private String roomNumber;
    private String playerId;
    private Integer seatNumber;
    private Boolean ready;
    private Integer players;
    /** 0 表示第一张是上身份，1 表示第二张。 */
    private Integer upperIndex;
}
