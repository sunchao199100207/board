package cn.game.board.model.avalon.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

/**
 * 玩家状态
 */
@Data
@NoArgsConstructor
public class Player {
    private String playerId;
    private String sessionId;
    private int seatNumber = 0;
    private boolean isReady = false;
    // 需要序列化到 Redis 以便重连时恢复
    private String identity;
    /**
     * 玩家首选身份
     */
    private String preferredRole;

    public Player(String sessionId) {
        this.sessionId = sessionId;
        this.playerId = UUID.randomUUID().toString();
    }
}
