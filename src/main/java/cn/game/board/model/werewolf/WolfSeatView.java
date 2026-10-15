package cn.game.board.model.werewolf;

import lombok.Builder;
import lombok.Data;

/**
 * 座位公开信息。故意不包含身份。
 */
@Data
@Builder
public class WolfSeatView {
    private int seat;
    private boolean occupied;
    private boolean ready;
    private boolean self;
    /** 法官能看到全部；玩家只能看到自己的。 */
    private String roleName;
    private String emoji;
    private String camp;
}
