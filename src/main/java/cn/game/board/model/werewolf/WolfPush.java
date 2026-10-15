package cn.game.board.model.werewolf;

import lombok.Builder;
import lombok.Data;

/**
 * 推到玩家私有频道的消息。type 为 state、error 或 closed。
 */
@Data
@Builder
public class WolfPush {
    private String type;
    private String error;
    private WolfRoomView state;
}
