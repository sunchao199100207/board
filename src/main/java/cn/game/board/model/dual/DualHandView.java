package cn.game.board.model.dual;

import lombok.Builder;
import lombok.Data;

/**
 * 只发给本人的两张牌。upperIndex 为空表示还没确认上身份。
 */
@Data
@Builder
public class DualHandView {
    private DualCardView cardA;
    private DualCardView cardB;
    private Integer upperIndex;
    private boolean confirmed;
    private String campName;
    private String campTip;
    private boolean golden;
}
