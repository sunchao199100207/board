package cn.game.board.model.werewolf;

import lombok.Data;

/**
 * 房间里的一名玩家。法官不占座，不会出现在这里。
 */
@Data
public class WolfPlayer {
    private String playerId;
    private int seatNumber;
    private boolean ready;
    /** 发牌后才有值，且只推给本人。 */
    private String role;
}
