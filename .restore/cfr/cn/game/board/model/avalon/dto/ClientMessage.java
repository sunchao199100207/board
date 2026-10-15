/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cn.game.board.model.avalon.dto.ClientMessage
 *  lombok.Generated
 */
package cn.game.board.model.avalon.dto;

import lombok.Generated;

public class ClientMessage {
    private String roomNumber;
    private int totalPlayers;
    private int seatNumber;
    private boolean isReady;
    private String playerId;
    private String targetPlayerId;
    private String preferredRole;

    public void setIsReady(boolean ready) {
        this.isReady = ready;
    }

    @Generated
    public String getRoomNumber() {
        return this.roomNumber;
    }

    @Generated
    public int getTotalPlayers() {
        return this.totalPlayers;
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
    public String getPlayerId() {
        return this.playerId;
    }

    @Generated
    public String getTargetPlayerId() {
        return this.targetPlayerId;
    }

    @Generated
    public String getPreferredRole() {
        return this.preferredRole;
    }

    @Generated
    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    @Generated
    public void setTotalPlayers(int totalPlayers) {
        this.totalPlayers = totalPlayers;
    }

    @Generated
    public void setSeatNumber(int seatNumber) {
        this.seatNumber = seatNumber;
    }

    @Generated
    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    @Generated
    public void setTargetPlayerId(String targetPlayerId) {
        this.targetPlayerId = targetPlayerId;
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
        if (!(o instanceof ClientMessage)) {
            return false;
        }
        ClientMessage other = (ClientMessage)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (this.getTotalPlayers() != other.getTotalPlayers()) {
            return false;
        }
        if (this.getSeatNumber() != other.getSeatNumber()) {
            return false;
        }
        if (this.isReady() != other.isReady()) {
            return false;
        }
        String this$roomNumber = this.getRoomNumber();
        String other$roomNumber = other.getRoomNumber();
        if (this$roomNumber == null ? other$roomNumber != null : !this$roomNumber.equals(other$roomNumber)) {
            return false;
        }
        String this$playerId = this.getPlayerId();
        String other$playerId = other.getPlayerId();
        if (this$playerId == null ? other$playerId != null : !this$playerId.equals(other$playerId)) {
            return false;
        }
        String this$targetPlayerId = this.getTargetPlayerId();
        String other$targetPlayerId = other.getTargetPlayerId();
        if (this$targetPlayerId == null ? other$targetPlayerId != null : !this$targetPlayerId.equals(other$targetPlayerId)) {
            return false;
        }
        String this$preferredRole = this.getPreferredRole();
        String other$preferredRole = other.getPreferredRole();
        return !(this$preferredRole == null ? other$preferredRole != null : !this$preferredRole.equals(other$preferredRole));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof ClientMessage;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getTotalPlayers();
        result = result * 59 + this.getSeatNumber();
        result = result * 59 + (this.isReady() ? 79 : 97);
        String $roomNumber = this.getRoomNumber();
        result = result * 59 + ($roomNumber == null ? 43 : $roomNumber.hashCode());
        String $playerId = this.getPlayerId();
        result = result * 59 + ($playerId == null ? 43 : $playerId.hashCode());
        String $targetPlayerId = this.getTargetPlayerId();
        result = result * 59 + ($targetPlayerId == null ? 43 : $targetPlayerId.hashCode());
        String $preferredRole = this.getPreferredRole();
        result = result * 59 + ($preferredRole == null ? 43 : $preferredRole.hashCode());
        return result;
    }

    @Generated
    public ClientMessage() {
    }

    @Generated
    public String toString() {
        return "ClientMessage(roomNumber=" + this.getRoomNumber() + ", totalPlayers=" + this.getTotalPlayers() + ", seatNumber=" + this.getSeatNumber() + ", isReady=" + this.isReady() + ", playerId=" + this.getPlayerId() + ", targetPlayerId=" + this.getTargetPlayerId() + ", preferredRole=" + this.getPreferredRole() + ")";
    }
}

