package cn.game.board.model.avalon.dto;

import lombok.Data;

/**
 * @author <a href="mailto:sunchao@gtmap.cn">sunchao</a
 * @version 1.0, 2025/10/27
 * @description
 */
@Data
public class IdentityRequest {
    private int playersNumber;
    private long gameNumber;
    private int seatNumber;
}
