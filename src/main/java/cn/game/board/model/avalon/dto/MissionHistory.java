package cn.game.board.model.avalon.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * 任务历史记录
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MissionHistory {
    private int round; // 轮次
    private Set<Integer> members; // 任务成员座位号
    private String result; // "success" 或 "fail"
}

