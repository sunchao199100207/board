package cn.game.board.model.avalon.dto;

import lombok.Data;

import java.util.List;

/**
 * 观战模式房间视图
 */
@Data
public class SpectatorRoomView {

    private String roomNumber;

    private boolean gameStarted;

    private int totalPlayers;

    private int currentLeaderSeat;

    private int currentMissionRound;

    private List<SpectatorPlayerView> players;
}

