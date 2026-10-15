package cn.game.board.model.werewolf;

import lombok.Data;

/**
 * 前端发来的发牌房间操作。
 */
@Data
public class WolfClientMessage {
    private String roomNumber;
    private String playerId;
    private Integer seatNumber;
    private Boolean ready;
    private WolfRoleConfig config;
}
