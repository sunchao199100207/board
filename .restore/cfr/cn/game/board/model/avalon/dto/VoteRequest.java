/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cn.game.board.model.avalon.dto.VoteRequest
 *  lombok.Generated
 */
package cn.game.board.model.avalon.dto;

import lombok.Generated;

public class VoteRequest {
    private String roomNumber;
    private String vote;

    @Generated
    public String getRoomNumber() {
        return this.roomNumber;
    }

    @Generated
    public String getVote() {
        return this.vote;
    }

    @Generated
    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    @Generated
    public void setVote(String vote) {
        this.vote = vote;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof VoteRequest)) {
            return false;
        }
        VoteRequest other = (VoteRequest)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        String this$roomNumber = this.getRoomNumber();
        String other$roomNumber = other.getRoomNumber();
        if (this$roomNumber == null ? other$roomNumber != null : !this$roomNumber.equals(other$roomNumber)) {
            return false;
        }
        String this$vote = this.getVote();
        String other$vote = other.getVote();
        return !(this$vote == null ? other$vote != null : !this$vote.equals(other$vote));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof VoteRequest;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $roomNumber = this.getRoomNumber();
        result = result * 59 + ($roomNumber == null ? 43 : $roomNumber.hashCode());
        String $vote = this.getVote();
        result = result * 59 + ($vote == null ? 43 : $vote.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "VoteRequest(roomNumber=" + this.getRoomNumber() + ", vote=" + this.getVote() + ")";
    }

    @Generated
    public VoteRequest() {
    }
}

