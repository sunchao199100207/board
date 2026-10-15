package cn.game.board.model.avalon.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 观战模式下的玩家视图
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpectatorPlayerView {

    private int seatNumber;

    private String playerId;

    private String identity;

    /**
     * 玩家阵营（good / evil）
     */
    private String camp;
}

