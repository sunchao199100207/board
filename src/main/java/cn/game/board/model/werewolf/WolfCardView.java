package cn.game.board.model.werewolf;

import lombok.Builder;
import lombok.Data;

/**
 * 只发给对应玩家的身份牌。
 */
@Data
@Builder
public class WolfCardView {
    private String role;
    private String roleName;
    private String emoji;
    private String camp;
    private String campName;
    private String hint;
    private String imageUrl;
}
