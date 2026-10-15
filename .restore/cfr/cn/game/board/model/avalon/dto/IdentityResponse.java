/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cn.game.board.model.avalon.dto.IdentityResponse
 *  cn.game.board.model.avalon.dto.IdentityResponse$IdentityResponseBuilder
 *  lombok.Generated
 */
package cn.game.board.model.avalon.dto;

import cn.game.board.model.avalon.dto.IdentityResponse;
import java.util.List;
import lombok.Generated;

public class IdentityResponse {
    private String identity;
    private String emoji;
    private String camp;
    private String seenTitle;
    private List<Integer> seenPlayers;
    private String seenSubtitle;
    private String imageUrl;
    private int leaderSeat;

    @Generated
    IdentityResponse(String identity, String emoji, String camp, String seenTitle, List<Integer> seenPlayers, String seenSubtitle, String imageUrl, int leaderSeat) {
        this.identity = identity;
        this.emoji = emoji;
        this.camp = camp;
        this.seenTitle = seenTitle;
        this.seenPlayers = seenPlayers;
        this.seenSubtitle = seenSubtitle;
        this.imageUrl = imageUrl;
        this.leaderSeat = leaderSeat;
    }

    @Generated
    public static IdentityResponseBuilder builder() {
        return new IdentityResponseBuilder();
    }

    @Generated
    public String getIdentity() {
        return this.identity;
    }

    @Generated
    public String getEmoji() {
        return this.emoji;
    }

    @Generated
    public String getCamp() {
        return this.camp;
    }

    @Generated
    public String getSeenTitle() {
        return this.seenTitle;
    }

    @Generated
    public List<Integer> getSeenPlayers() {
        return this.seenPlayers;
    }

    @Generated
    public String getSeenSubtitle() {
        return this.seenSubtitle;
    }

    @Generated
    public String getImageUrl() {
        return this.imageUrl;
    }

    @Generated
    public int getLeaderSeat() {
        return this.leaderSeat;
    }

    @Generated
    public void setIdentity(String identity) {
        this.identity = identity;
    }

    @Generated
    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    @Generated
    public void setCamp(String camp) {
        this.camp = camp;
    }

    @Generated
    public void setSeenTitle(String seenTitle) {
        this.seenTitle = seenTitle;
    }

    @Generated
    public void setSeenPlayers(List<Integer> seenPlayers) {
        this.seenPlayers = seenPlayers;
    }

    @Generated
    public void setSeenSubtitle(String seenSubtitle) {
        this.seenSubtitle = seenSubtitle;
    }

    @Generated
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Generated
    public void setLeaderSeat(int leaderSeat) {
        this.leaderSeat = leaderSeat;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof IdentityResponse)) {
            return false;
        }
        IdentityResponse other = (IdentityResponse)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (this.getLeaderSeat() != other.getLeaderSeat()) {
            return false;
        }
        String this$identity = this.getIdentity();
        String other$identity = other.getIdentity();
        if (this$identity == null ? other$identity != null : !this$identity.equals(other$identity)) {
            return false;
        }
        String this$emoji = this.getEmoji();
        String other$emoji = other.getEmoji();
        if (this$emoji == null ? other$emoji != null : !this$emoji.equals(other$emoji)) {
            return false;
        }
        String this$camp = this.getCamp();
        String other$camp = other.getCamp();
        if (this$camp == null ? other$camp != null : !this$camp.equals(other$camp)) {
            return false;
        }
        String this$seenTitle = this.getSeenTitle();
        String other$seenTitle = other.getSeenTitle();
        if (this$seenTitle == null ? other$seenTitle != null : !this$seenTitle.equals(other$seenTitle)) {
            return false;
        }
        List this$seenPlayers = this.getSeenPlayers();
        List other$seenPlayers = other.getSeenPlayers();
        if (this$seenPlayers == null ? other$seenPlayers != null : !((Object)this$seenPlayers).equals(other$seenPlayers)) {
            return false;
        }
        String this$seenSubtitle = this.getSeenSubtitle();
        String other$seenSubtitle = other.getSeenSubtitle();
        if (this$seenSubtitle == null ? other$seenSubtitle != null : !this$seenSubtitle.equals(other$seenSubtitle)) {
            return false;
        }
        String this$imageUrl = this.getImageUrl();
        String other$imageUrl = other.getImageUrl();
        return !(this$imageUrl == null ? other$imageUrl != null : !this$imageUrl.equals(other$imageUrl));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof IdentityResponse;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getLeaderSeat();
        String $identity = this.getIdentity();
        result = result * 59 + ($identity == null ? 43 : $identity.hashCode());
        String $emoji = this.getEmoji();
        result = result * 59 + ($emoji == null ? 43 : $emoji.hashCode());
        String $camp = this.getCamp();
        result = result * 59 + ($camp == null ? 43 : $camp.hashCode());
        String $seenTitle = this.getSeenTitle();
        result = result * 59 + ($seenTitle == null ? 43 : $seenTitle.hashCode());
        List $seenPlayers = this.getSeenPlayers();
        result = result * 59 + ($seenPlayers == null ? 43 : ((Object)$seenPlayers).hashCode());
        String $seenSubtitle = this.getSeenSubtitle();
        result = result * 59 + ($seenSubtitle == null ? 43 : $seenSubtitle.hashCode());
        String $imageUrl = this.getImageUrl();
        result = result * 59 + ($imageUrl == null ? 43 : $imageUrl.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "IdentityResponse(identity=" + this.getIdentity() + ", emoji=" + this.getEmoji() + ", camp=" + this.getCamp() + ", seenTitle=" + this.getSeenTitle() + ", seenPlayers=" + String.valueOf(this.getSeenPlayers()) + ", seenSubtitle=" + this.getSeenSubtitle() + ", imageUrl=" + this.getImageUrl() + ", leaderSeat=" + this.getLeaderSeat() + ")";
    }
}

