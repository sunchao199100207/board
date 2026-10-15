/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cn.game.board.model.avalon.dto.MissionRequest
 *  lombok.Generated
 */
package cn.game.board.model.avalon.dto;

import lombok.Generated;

public class MissionRequest {
    private String roomNumber;
    private boolean joinMission;

    @Generated
    public String getRoomNumber() {
        return this.roomNumber;
    }

    @Generated
    public boolean isJoinMission() {
        return this.joinMission;
    }

    @Generated
    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    @Generated
    public void setJoinMission(boolean joinMission) {
        this.joinMission = joinMission;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof MissionRequest)) {
            return false;
        }
        MissionRequest other = (MissionRequest)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (this.isJoinMission() != other.isJoinMission()) {
            return false;
        }
        String this$roomNumber = this.getRoomNumber();
        String other$roomNumber = other.getRoomNumber();
        return !(this$roomNumber == null ? other$roomNumber != null : !this$roomNumber.equals(other$roomNumber));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof MissionRequest;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isJoinMission() ? 79 : 97);
        String $roomNumber = this.getRoomNumber();
        result = result * 59 + ($roomNumber == null ? 43 : $roomNumber.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "MissionRequest(roomNumber=" + this.getRoomNumber() + ", joinMission=" + this.isJoinMission() + ")";
    }

    @Generated
    public MissionRequest() {
    }
}

