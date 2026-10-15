package cn.game.board.model.dual;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DualCardView {
    private String role;
    private String roleName;
    private String emoji;
    private String camp;
    private String hint;
    private String imageUrl;
}
