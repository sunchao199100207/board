/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cn.game.board.model.avalon.dto.Player
 *  lombok.Generated
 */
package cn.game.board.model.avalon.dto;

import java.util.UUID;
import lombok.Generated;

public class Player {
    private String playerId;
    private String sessionId;
    private int seatNumber = 0;
    private boolean isReady = false;
    private String identity;
    private String preferredRole;

    public Player(String sessionId) {
        this.sessionId = sessionId;
        this.playerId = UUID.randomUUID().toString();
    }

    @Generated
    public String getPlayerId() {
        return this.playerId;
    }

    @Generated
    public String getSessionId() {
        return this.sessionId;
    }

    @Generated
    public int getSeatNumber() {
        return this.seatNumber;
    }

    @Generated
    public boolean isReady() {
        return this.isReady;
    }

    @Generated
    public String getIdentity() {
        return this.identity;
    }

    @Generated
    public String getPreferredRole() {
        return this.preferredRole;
    }

    @Generated
    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    @Generated
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Generated
    public void setSeatNumber(int seatNumber) {
        this.seatNumber = seatNumber;
    }

    @Generated
    public void setReady(boolean isReady) {
        this.isReady = isReady;
    }

    @Generated
    public void setIdentity(String identity) {
        this.identity = identity;
    }

    @Generated
    public void setPreferredRole(String preferredRole) {
        this.preferredRole = preferredRole;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Player)) {
            return false;
        }
        Player other = (Player)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (this.getSeatNumber() != other.getSeatNumber()) {
            return false;
        }
        if (this.isReady() != other.isReady()) {
            return false;
        }
        String this$playerId = this.getPlayerId();
        String other$playerId = other.getPlayerId();
        if (this$playerId == null ? other$playerId != null : !this$playerId.equals(other$playerId)) {
            return false;
        }
        String this$sessionId = this.getSessionId();
        String other$sessionId = other.getSessionId();
        if (this$sessionId == null ? other$sessionId != null : !this$sessionId.equals(other$sessionId)) {
            return false;
        }
        String this$identity = this.getIdentity();
        String other$identity = other.getIdentity();
        if (this$identity == null ? other$identity != null : !this$identity.equals(other$identity)) {
            return false;
        }
        String this$preferredRole = this.getPreferredRole();
        String other$preferredRole = other.getPreferredRole();
        return !(this$preferredRole == null ? other$preferredRole != null : !this$preferredRole.equals(other$preferredRole));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof Player;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getSeatNumber();
        result = result * 59 + (this.isReady() ? 79 : 97);
        String $playerId = this.getPlayerId();
        result = result * 59 + ($playerId == null ? 43 : $playerId.hashCode());
        String $sessionId = this.getSessionId();
        result = result * 59 + ($sessionId == null ? 43 : $sessionId.hashCode());
        String $identity = this.getIdentity();
        result = result * 59 + ($identity == null ? 43 : $identity.hashCode());
        String $preferredRole = this.getPreferredRole();
        result = result * 59 + ($preferredRole == null ? 43 : $preferredRole.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "Player(playerId=" + this.getPlayerId() + ", sessionId=" + this.getSessionId() + ", seatNumber=" + this.getSeatNumber() + ", isReady=" + this.isReady() + ", identity=" + this.getIdentity() + ", preferredRole=" + this.getPreferredRole() + ")";
    }

    @Generated
    public Player() {
    }
}

