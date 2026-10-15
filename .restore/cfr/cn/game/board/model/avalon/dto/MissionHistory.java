/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cn.game.board.model.avalon.dto.MissionHistory
 *  lombok.Generated
 */
package cn.game.board.model.avalon.dto;

import java.util.Set;
import lombok.Generated;

public class MissionHistory {
    private int round;
    private Set<Integer> members;
    private String result;

    @Generated
    public int getRound() {
        return this.round;
    }

    @Generated
    public Set<Integer> getMembers() {
        return this.members;
    }

    @Generated
    public String getResult() {
        return this.result;
    }

    @Generated
    public void setRound(int round) {
        this.round = round;
    }

    @Generated
    public void setMembers(Set<Integer> members) {
        this.members = members;
    }

    @Generated
    public void setResult(String result) {
        this.result = result;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof MissionHistory)) {
            return false;
        }
        MissionHistory other = (MissionHistory)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (this.getRound() != other.getRound()) {
            return false;
        }
        Set this$members = this.getMembers();
        Set other$members = other.getMembers();
        if (this$members == null ? other$members != null : !((Object)this$members).equals(other$members)) {
            return false;
        }
        String this$result = this.getResult();
        String other$result = other.getResult();
        return !(this$result == null ? other$result != null : !this$result.equals(other$result));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof MissionHistory;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getRound();
        Set $members = this.getMembers();
        result = result * 59 + ($members == null ? 43 : ((Object)$members).hashCode());
        String $result = this.getResult();
        result = result * 59 + ($result == null ? 43 : $result.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "MissionHistory(round=" + this.getRound() + ", members=" + String.valueOf(this.getMembers()) + ", result=" + this.getResult() + ")";
    }

    @Generated
    public MissionHistory() {
    }

    @Generated
    public MissionHistory(int round, Set<Integer> members, String result) {
        this.round = round;
        this.members = members;
        this.result = result;
    }
}

