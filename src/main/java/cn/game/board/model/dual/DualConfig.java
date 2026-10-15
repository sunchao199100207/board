package cn.game.board.model.dual;

import lombok.Data;

/**
 * 法官选的人数。6、7、8 人各有一套固定牌，张数始终是人数的两倍。
 */
@Data
public class DualConfig {
    private int players = 6;
}
