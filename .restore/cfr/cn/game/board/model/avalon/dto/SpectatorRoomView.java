/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cn.game.board.model.avalon.dto.SpectatorPlayerView
 *  cn.game.board.model.avalon.dto.SpectatorRoomView
 *  lombok.Generated
 */
package cn.game.board.model.avalon.dto;

import cn.game.board.model.avalon.dto.SpectatorPlayerView;
import java.util.List;
import lombok.Generated;

public class SpectatorRoomView {
    private String roomNumber;
    private boolean gameStarted;
    private int totalPlayers;
    private int currentLeaderSeat;
    private int currentMissionRound;
    private List<SpectatorPlayerView> players;

    @Generated
    public String getRoomNumber() {
        return this.roomNumber;
    }

    @Generated
    public boolean isGameStarted() {
        return this.gameStarted;
    }

    @Generated
    public int getTotalPlayers() {
        return this.totalPlayers;
    }

    @Generated
    public int getCurrentLeaderSeat() {
        return this.currentLeaderSeat;
    }

    @Generated
    public int getCurrentMissionRound() {
        return this.currentMissionRound;
    }

    @Generated
    public List<SpectatorPlayerView> getPlayers() {
        return this.players;
    }

    @Generated
    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    @Generated
    public void setGameStarted(boolean gameStarted) {
        this.gameStarted = gameStarted;
    }

    @Generated
    public void setTotalPlayers(int totalPlayers) {
        this.totalPlayers = totalPlayers;
    }

    @Generated
    public void setCurrentLeaderSeat(int currentLeaderSeat) {
        this.currentLeaderSeat = currentLeaderSeat;
    }

    @Generated
    public void setCurrentMissionRound(int currentMissionRound) {
        this.currentMissionRound = currentMissionRound;
    }

    @Generated
    public void setPlayers(List<SpectatorPlayerView> players) {
        this.players = players;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SpectatorRoomView)) {
            return false;
        }
        SpectatorRoomView other = (SpectatorRoomView)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (this.isGameStarted() != other.isGameStarted()) {
            return false;
        }
        if (this.getTotalPlayers() != other.getTotalPlayers()) {
            return false;
        }
        if (this.getCurrentLeaderSeat() != other.getCurrentLeaderSeat()) {
            return false;
        }
        if (this.getCurrentMissionRound() != other.getCurrentMissionRound()) {
            return false;
        }
        String this$roomNumber = this.getRoomNumber();
        String other$roomNumber = other.getRoomNumber();
        if (this$roomNumber == null ? other$roomNumber != null : !this$roomNumber.equals(other$roomNumber)) {
            return false;
        }
        List this$players = this.getPlayers();
        List other$players = other.getPlayers();
        return !(this$players == null ? other$players != null : !((Object)this$players).equals(other$players));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SpectatorRoomView;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isGameStarted() ? 79 : 97);
        result = result * 59 + this.getTotalPlayers();
        result = result * 59 + this.getCurrentLeaderSeat();
        result = result * 59 + this.getCurrentMissionRound();
        String $roomNumber = this.getRoomNumber();
        result = result * 59 + ($roomNumber == null ? 43 : $roomNumber.hashCode());
        List $players = this.getPlayers();
        result = result * 59 + ($players == null ? 43 : ((Object)$players).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "SpectatorRoomView(roomNumber=" + this.getRoomNumber() + ", gameStarted=" + this.isGameStarted() + ", totalPlayers=" + this.getTotalPlayers() + ", currentLeaderSeat=" + this.getCurrentLeaderSeat() + ", currentMissionRound=" + this.getCurrentMissionRound() + ", players=" + String.valueOf(this.getPlayers()) + ")";
    }

    @Generated
    public SpectatorRoomView() {
    }
}

