package cn.game.board.model.avalon.dto;

import lombok.Data;

/**
 * 投票请求
 */
@Data
public class VoteRequest {
    private String roomNumber;
    private String vote; // "success" 或 "fail"
}

