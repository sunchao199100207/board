/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cn.game.board.model.avalon.dto.IdentityRequest
 *  lombok.Generated
 */
package cn.game.board.model.avalon.dto;

import lombok.Generated;

public class IdentityRequest {
    private int playersNumber;
    private long gameNumber;
    private int seatNumber;

    @Generated
    public int getPlayersNumber() {
        return this.playersNumber;
    }

    @Generated
    public long getGameNumber() {
        return this.gameNumber;
    }

    @Generated
    public int getSeatNumber() {
        return this.seatNumber;
    }

    @Generated
    public void setPlayersNumber(int playersNumber) {
        this.playersNumber = playersNumber;
    }

    @Generated
    public void setGameNumber(long gameNumber) {
        this.gameNumber = gameNumber;
    }

    @Generated
    public void setSeatNumber(int seatNumber) {
        this.seatNumber = seatNumber;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof IdentityRequest)) {
            return false;
        }
        IdentityRequest other = (IdentityRequest)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (this.getPlayersNumber() != other.getPlayersNumber()) {
            return false;
        }
        if (this.getGameNumber() != other.getGameNumber()) {
            return false;
        }
        return this.getSeatNumber() == other.getSeatNumber();
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof IdentityRequest;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getPlayersNumber();
        long $gameNumber = this.getGameNumber();
        result = result * 59 + (int)($gameNumber ^ $gameNumber >>> 32);
        result = result * 59 + this.getSeatNumber();
        return result;
    }

    @Generated
    public String toString() {
        return "IdentityRequest(playersNumber=" + this.getPlayersNumber() + ", gameNumber=" + this.getGameNumber() + ", seatNumber=" + this.getSeatNumber() + ")";
    }

    @Generated
    public IdentityRequest() {
    }
}

