package cn.game.board.model.avalon.dto;

import lombok.Data;

/**
 * 任务请求（玩家自己选择是否上车）
 */
@Data
public class MissionRequest {
    private String roomNumber;
    private boolean joinMission; // true=上车, false=下车
}

