/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cn.game.board.model.avalon.dto.SpectatorPlayerView
 */
package cn.game.board.model.avalon.dto;

public class SpectatorPlayerView {
    private int seatNumber;
    private String playerId;
    private String identity;
    private String camp;

    public int getSeatNumber() {
        return this.seatNumber;
    }

    public String getPlayerId() {
        return this.playerId;
    }

    public String getIdentity() {
        return this.identity;
    }

    public String getCamp() {
        return this.camp;
    }

    public void setSeatNumber(int seatNumber) {
        this.seatNumber = seatNumber;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public void setCamp(String camp) {
        this.camp = camp;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SpectatorPlayerView)) {
            return false;
        }
        SpectatorPlayerView other = (SpectatorPlayerView)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (this.getSeatNumber() != other.getSeatNumber()) {
            return false;
        }
        String this$playerId = this.getPlayerId();
        String other$playerId = other.getPlayerId();
        if (this$playerId == null ? other$playerId != null : !this$playerId.equals(other$playerId)) {
            return false;
        }
        String this$identity = this.getIdentity();
        String other$identity = other.getIdentity();
        if (this$identity == null ? other$identity != null : !this$identity.equals(other$identity)) {
            return false;
        }
        String this$camp = this.getCamp();
        String other$camp = other.getCamp();
        return !(this$camp == null ? other$camp != null : !this$camp.equals(other$camp));
    }

    protected boolean canEqual(Object other) {
        return other instanceof SpectatorPlayerView;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getSeatNumber();
        String $playerId = this.getPlayerId();
        result = result * 59 + ($playerId == null ? 43 : $playerId.hashCode());
        String $identity = this.getIdentity();
        result = result * 59 + ($identity == null ? 43 : $identity.hashCode());
        String $camp = this.getCamp();
        result = result * 59 + ($camp == null ? 43 : $camp.hashCode());
        return result;
    }

    public String toString() {
        return "SpectatorPlayerView(seatNumber=" + this.getSeatNumber() + ", playerId=" + this.getPlayerId() + ", identity=" + this.getIdentity() + ", camp=" + this.getCamp() + ")";
    }

    public SpectatorPlayerView() {
    }

    public SpectatorPlayerView(int seatNumber, String playerId, String identity, String camp) {
        this.seatNumber = seatNumber;
        this.playerId = playerId;
        this.identity = identity;
        this.camp = camp;
    }
}

