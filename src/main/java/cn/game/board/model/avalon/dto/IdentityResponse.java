package cn.game.board.model.avalon.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author <a href="mailto:sunchao@gtmap.cn">sunchao</a
 * @version 1.0, 2025/10/27
 * @description
 */
@Data
@Builder
public class IdentityResponse {
    private String identity;
    private String emoji;
    private String camp;
    private String seenTitle;
    private List<Integer> seenPlayers;
    private String seenSubtitle;
    private String imageUrl;
    private int leaderSeat;
}
