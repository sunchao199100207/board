package cn.game.board.model.avalon.dto;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class ClientMessage {
    private String roomNumber;
    private int totalPlayers;
    private int seatNumber;
    private boolean isReady;
    private String playerId; // 用于重连

    // --- 新增：用于房主操作 ---
    private String targetPlayerId; // 目标玩家的 Player ID
    // --- 结束新增 ---

    /**
     * 首选身份（来自前端偏好设置）
     */
    private String preferredRole;

    public void setIsReady(boolean ready) {
        this.isReady = ready;
    }
}