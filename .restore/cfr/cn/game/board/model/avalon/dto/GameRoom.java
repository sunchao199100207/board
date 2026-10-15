/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cn.game.board.model.avalon.dto.GameRoom
 *  cn.game.board.model.avalon.dto.MissionHistory
 *  cn.game.board.model.avalon.dto.Player
 *  lombok.Generated
 */
package cn.game.board.model.avalon.dto;

import cn.game.board.model.avalon.dto.MissionHistory;
import cn.game.board.model.avalon.dto.Player;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Generated;

public class GameRoom {
    private String roomNumber;
    private int totalPlayers;
    private String hostPlayerId;
    private boolean gameStarted = false;
    private Map<String, Player> players = new ConcurrentHashMap();
    private Map<String, Player> initialPlayers = new ConcurrentHashMap();
    private int gameCount = 0;
    private List<String> shuffledIdentities;
    private int currentLeaderSeat;
    private int currentMissionRound = 0;
    private int currentLeaderIndex = 0;
    private Set<Integer> selectedMissionMembers = new HashSet();
    private String missionProposerSessionId;
    private Map<String, String> votes = new ConcurrentHashMap();
    private boolean missionVotingCompleted = false;
    private String missionResult;
    private int missionFailCount = 0;
    private List<MissionHistory> missionHistory = new ArrayList();
    private Integer lakeLadyHolderSeat = null;
    private Set<Integer> lakeLadyUsedSeats = new HashSet();
    private Map<Integer, Integer> lakeLadyInspectionHistory = new HashMap();
    private Map<Integer, String> lakeLadyInspectionResult = new HashMap();
    private Map<Integer, String> lakeLadyPublicResult = new HashMap();
    private Integer lakeLadyLastInspectorSeat = null;

    public GameRoom(String roomNumber, int totalPlayers, String hostSessionId) {
        this.roomNumber = roomNumber;
        this.totalPlayers = totalPlayers;
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
    public String getHostPlayerId() {
        return this.hostPlayerId;
    }

    @Generated
    public boolean isGameStarted() {
        return this.gameStarted;
    }

    @Generated
    public Map<String, Player> getPlayers() {
        return this.players;
    }

    @Generated
    public Map<String, Player> getInitialPlayers() {
        return this.initialPlayers;
    }

    @Generated
    public int getGameCount() {
        return this.gameCount;
    }

    @Generated
    public List<String> getShuffledIdentities() {
        return this.shuffledIdentities;
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
    public int getCurrentLeaderIndex() {
        return this.currentLeaderIndex;
    }

    @Generated
    public Set<Integer> getSelectedMissionMembers() {
        return this.selectedMissionMembers;
    }

    @Generated
    public String getMissionProposerSessionId() {
        return this.missionProposerSessionId;
    }

    @Generated
    public Map<String, String> getVotes() {
        return this.votes;
    }

    @Generated
    public boolean isMissionVotingCompleted() {
        return this.missionVotingCompleted;
    }

    @Generated
    public String getMissionResult() {
        return this.missionResult;
    }

    @Generated
    public int getMissionFailCount() {
        return this.missionFailCount;
    }

    @Generated
    public List<MissionHistory> getMissionHistory() {
        return this.missionHistory;
    }

    @Generated
    public Integer getLakeLadyHolderSeat() {
        return this.lakeLadyHolderSeat;
    }

    @Generated
    public Set<Integer> getLakeLadyUsedSeats() {
        return this.lakeLadyUsedSeats;
    }

    @Generated
    public Map<Integer, Integer> getLakeLadyInspectionHistory() {
        return this.lakeLadyInspectionHistory;
    }

    @Generated
    public Map<Integer, String> getLakeLadyInspectionResult() {
        return this.lakeLadyInspectionResult;
    }

    @Generated
    public Map<Integer, String> getLakeLadyPublicResult() {
        return this.lakeLadyPublicResult;
    }

    @Generated
    public Integer getLakeLadyLastInspectorSeat() {
        return this.lakeLadyLastInspectorSeat;
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
    public void setHostPlayerId(String hostPlayerId) {
        this.hostPlayerId = hostPlayerId;
    }

    @Generated
    public void setGameStarted(boolean gameStarted) {
        this.gameStarted = gameStarted;
    }

    @Generated
    public void setPlayers(Map<String, Player> players) {
        this.players = players;
    }

    @Generated
    public void setInitialPlayers(Map<String, Player> initialPlayers) {
        this.initialPlayers = initialPlayers;
    }

    @Generated
    public void setGameCount(int gameCount) {
        this.gameCount = gameCount;
    }

    @Generated
    public void setShuffledIdentities(List<String> shuffledIdentities) {
        this.shuffledIdentities = shuffledIdentities;
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
    public void setCurrentLeaderIndex(int currentLeaderIndex) {
        this.currentLeaderIndex = currentLeaderIndex;
    }

    @Generated
    public void setSelectedMissionMembers(Set<Integer> selectedMissionMembers) {
        this.selectedMissionMembers = selectedMissionMembers;
    }

    @Generated
    public void setMissionProposerSessionId(String missionProposerSessionId) {
        this.missionProposerSessionId = missionProposerSessionId;
    }

    @Generated
    public void setVotes(Map<String, String> votes) {
        this.votes = votes;
    }

    @Generated
    public void setMissionVotingCompleted(boolean missionVotingCompleted) {
        this.missionVotingCompleted = missionVotingCompleted;
    }

    @Generated
    public void setMissionResult(String missionResult) {
        this.missionResult = missionResult;
    }

    @Generated
    public void setMissionFailCount(int missionFailCount) {
        this.missionFailCount = missionFailCount;
    }

    @Generated
    public void setMissionHistory(List<MissionHistory> missionHistory) {
        this.missionHistory = missionHistory;
    }

    @Generated
    public void setLakeLadyHolderSeat(Integer lakeLadyHolderSeat) {
        this.lakeLadyHolderSeat = lakeLadyHolderSeat;
    }

    @Generated
    public void setLakeLadyUsedSeats(Set<Integer> lakeLadyUsedSeats) {
        this.lakeLadyUsedSeats = lakeLadyUsedSeats;
    }

    @Generated
    public void setLakeLadyInspectionHistory(Map<Integer, Integer> lakeLadyInspectionHistory) {
        this.lakeLadyInspectionHistory = lakeLadyInspectionHistory;
    }

    @Generated
    public void setLakeLadyInspectionResult(Map<Integer, String> lakeLadyInspectionResult) {
        this.lakeLadyInspectionResult = lakeLadyInspectionResult;
    }

    @Generated
    public void setLakeLadyPublicResult(Map<Integer, String> lakeLadyPublicResult) {
        this.lakeLadyPublicResult = lakeLadyPublicResult;
    }

    @Generated
    public void setLakeLadyLastInspectorSeat(Integer lakeLadyLastInspectorSeat) {
        this.lakeLadyLastInspectorSeat = lakeLadyLastInspectorSeat;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof GameRoom)) {
            return false;
        }
        GameRoom other = (GameRoom)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (this.getTotalPlayers() != other.getTotalPlayers()) {
            return false;
        }
        if (this.isGameStarted() != other.isGameStarted()) {
            return false;
        }
        if (this.getGameCount() != other.getGameCount()) {
            return false;
        }
        if (this.getCurrentLeaderSeat() != other.getCurrentLeaderSeat()) {
            return false;
        }
        if (this.getCurrentMissionRound() != other.getCurrentMissionRound()) {
            return false;
        }
        if (this.getCurrentLeaderIndex() != other.getCurrentLeaderIndex()) {
            return false;
        }
        if (this.isMissionVotingCompleted() != other.isMissionVotingCompleted()) {
            return false;
        }
        if (this.getMissionFailCount() != other.getMissionFailCount()) {
            return false;
        }
        Integer this$lakeLadyHolderSeat = this.getLakeLadyHolderSeat();
        Integer other$lakeLadyHolderSeat = other.getLakeLadyHolderSeat();
        if (this$lakeLadyHolderSeat == null ? other$lakeLadyHolderSeat != null : !((Object)this$lakeLadyHolderSeat).equals(other$lakeLadyHolderSeat)) {
            return false;
        }
        Integer this$lakeLadyLastInspectorSeat = this.getLakeLadyLastInspectorSeat();
        Integer other$lakeLadyLastInspectorSeat = other.getLakeLadyLastInspectorSeat();
        if (this$lakeLadyLastInspectorSeat == null ? other$lakeLadyLastInspectorSeat != null : !((Object)this$lakeLadyLastInspectorSeat).equals(other$lakeLadyLastInspectorSeat)) {
            return false;
        }
        String this$roomNumber = this.getRoomNumber();
        String other$roomNumber = other.getRoomNumber();
        if (this$roomNumber == null ? other$roomNumber != null : !this$roomNumber.equals(other$roomNumber)) {
            return false;
        }
        String this$hostPlayerId = this.getHostPlayerId();
        String other$hostPlayerId = other.getHostPlayerId();
        if (this$hostPlayerId == null ? other$hostPlayerId != null : !this$hostPlayerId.equals(other$hostPlayerId)) {
            return false;
        }
        Map this$players = this.getPlayers();
        Map other$players = other.getPlayers();
        if (this$players == null ? other$players != null : !((Object)this$players).equals(other$players)) {
            return false;
        }
        Map this$initialPlayers = this.getInitialPlayers();
        Map other$initialPlayers = other.getInitialPlayers();
        if (this$initialPlayers == null ? other$initialPlayers != null : !((Object)this$initialPlayers).equals(other$initialPlayers)) {
            return false;
        }
        List this$shuffledIdentities = this.getShuffledIdentities();
        List other$shuffledIdentities = other.getShuffledIdentities();
        if (this$shuffledIdentities == null ? other$shuffledIdentities != null : !((Object)this$shuffledIdentities).equals(other$shuffledIdentities)) {
            return false;
        }
        Set this$selectedMissionMembers = this.getSelectedMissionMembers();
        Set other$selectedMissionMembers = other.getSelectedMissionMembers();
        if (this$selectedMissionMembers == null ? other$selectedMissionMembers != null : !((Object)this$selectedMissionMembers).equals(other$selectedMissionMembers)) {
            return false;
        }
        String this$missionProposerSessionId = this.getMissionProposerSessionId();
        String other$missionProposerSessionId = other.getMissionProposerSessionId();
        if (this$missionProposerSessionId == null ? other$missionProposerSessionId != null : !this$missionProposerSessionId.equals(other$missionProposerSessionId)) {
            return false;
        }
        Map this$votes = this.getVotes();
        Map other$votes = other.getVotes();
        if (this$votes == null ? other$votes != null : !((Object)this$votes).equals(other$votes)) {
            return false;
        }
        String this$missionResult = this.getMissionResult();
        String other$missionResult = other.getMissionResult();
        if (this$missionResult == null ? other$missionResult != null : !this$missionResult.equals(other$missionResult)) {
            return false;
        }
        List this$missionHistory = this.getMissionHistory();
        List other$missionHistory = other.getMissionHistory();
        if (this$missionHistory == null ? other$missionHistory != null : !((Object)this$missionHistory).equals(other$missionHistory)) {
            return false;
        }
        Set this$lakeLadyUsedSeats = this.getLakeLadyUsedSeats();
        Set other$lakeLadyUsedSeats = other.getLakeLadyUsedSeats();
        if (this$lakeLadyUsedSeats == null ? other$lakeLadyUsedSeats != null : !((Object)this$lakeLadyUsedSeats).equals(other$lakeLadyUsedSeats)) {
            return false;
        }
        Map this$lakeLadyInspectionHistory = this.getLakeLadyInspectionHistory();
        Map other$lakeLadyInspectionHistory = other.getLakeLadyInspectionHistory();
        if (this$lakeLadyInspectionHistory == null ? other$lakeLadyInspectionHistory != null : !((Object)this$lakeLadyInspectionHistory).equals(other$lakeLadyInspectionHistory)) {
            return false;
        }
        Map this$lakeLadyInspectionResult = this.getLakeLadyInspectionResult();
        Map other$lakeLadyInspectionResult = other.getLakeLadyInspectionResult();
        if (this$lakeLadyInspectionResult == null ? other$lakeLadyInspectionResult != null : !((Object)this$lakeLadyInspectionResult).equals(other$lakeLadyInspectionResult)) {
            return false;
        }
        Map this$lakeLadyPublicResult = this.getLakeLadyPublicResult();
        Map other$lakeLadyPublicResult = other.getLakeLadyPublicResult();
        return !(this$lakeLadyPublicResult == null ? other$lakeLadyPublicResult != null : !((Object)this$lakeLadyPublicResult).equals(other$lakeLadyPublicResult));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof GameRoom;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getTotalPlayers();
        result = result * 59 + (this.isGameStarted() ? 79 : 97);
        result = result * 59 + this.getGameCount();
        result = result * 59 + this.getCurrentLeaderSeat();
        result = result * 59 + this.getCurrentMissionRound();
        result = result * 59 + this.getCurrentLeaderIndex();
        result = result * 59 + (this.isMissionVotingCompleted() ? 79 : 97);
        result = result * 59 + this.getMissionFailCount();
        Integer $lakeLadyHolderSeat = this.getLakeLadyHolderSeat();
        result = result * 59 + ($lakeLadyHolderSeat == null ? 43 : ((Object)$lakeLadyHolderSeat).hashCode());
        Integer $lakeLadyLastInspectorSeat = this.getLakeLadyLastInspectorSeat();
        result = result * 59 + ($lakeLadyLastInspectorSeat == null ? 43 : ((Object)$lakeLadyLastInspectorSeat).hashCode());
        String $roomNumber = this.getRoomNumber();
        result = result * 59 + ($roomNumber == null ? 43 : $roomNumber.hashCode());
        String $hostPlayerId = this.getHostPlayerId();
        result = result * 59 + ($hostPlayerId == null ? 43 : $hostPlayerId.hashCode());
        Map $players = this.getPlayers();
        result = result * 59 + ($players == null ? 43 : ((Object)$players).hashCode());
        Map $initialPlayers = this.getInitialPlayers();
        result = result * 59 + ($initialPlayers == null ? 43 : ((Object)$initialPlayers).hashCode());
        List $shuffledIdentities = this.getShuffledIdentities();
        result = result * 59 + ($shuffledIdentities == null ? 43 : ((Object)$shuffledIdentities).hashCode());
        Set $selectedMissionMembers = this.getSelectedMissionMembers();
        result = result * 59 + ($selectedMissionMembers == null ? 43 : ((Object)$selectedMissionMembers).hashCode());
        String $missionProposerSessionId = this.getMissionProposerSessionId();
        result = result * 59 + ($missionProposerSessionId == null ? 43 : $missionProposerSessionId.hashCode());
        Map $votes = this.getVotes();
        result = result * 59 + ($votes == null ? 43 : ((Object)$votes).hashCode());
        String $missionResult = this.getMissionResult();
        result = result * 59 + ($missionResult == null ? 43 : $missionResult.hashCode());
        List $missionHistory = this.getMissionHistory();
        result = result * 59 + ($missionHistory == null ? 43 : ((Object)$missionHistory).hashCode());
        Set $lakeLadyUsedSeats = this.getLakeLadyUsedSeats();
        result = result * 59 + ($lakeLadyUsedSeats == null ? 43 : ((Object)$lakeLadyUsedSeats).hashCode());
        Map $lakeLadyInspectionHistory = this.getLakeLadyInspectionHistory();
        result = result * 59 + ($lakeLadyInspectionHistory == null ? 43 : ((Object)$lakeLadyInspectionHistory).hashCode());
        Map $lakeLadyInspectionResult = this.getLakeLadyInspectionResult();
        result = result * 59 + ($lakeLadyInspectionResult == null ? 43 : ((Object)$lakeLadyInspectionResult).hashCode());
        Map $lakeLadyPublicResult = this.getLakeLadyPublicResult();
        result = result * 59 + ($lakeLadyPublicResult == null ? 43 : ((Object)$lakeLadyPublicResult).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "GameRoom(roomNumber=" + this.getRoomNumber() + ", totalPlayers=" + this.getTotalPlayers() + ", hostPlayerId=" + this.getHostPlayerId() + ", gameStarted=" + this.isGameStarted() + ", players=" + String.valueOf(this.getPlayers()) + ", initialPlayers=" + String.valueOf(this.getInitialPlayers()) + ", gameCount=" + this.getGameCount() + ", shuffledIdentities=" + String.valueOf(this.getShuffledIdentities()) + ", currentLeaderSeat=" + this.getCurrentLeaderSeat() + ", currentMissionRound=" + this.getCurrentMissionRound() + ", currentLeaderIndex=" + this.getCurrentLeaderIndex() + ", selectedMissionMembers=" + String.valueOf(this.getSelectedMissionMembers()) + ", missionProposerSessionId=" + this.getMissionProposerSessionId() + ", votes=" + String.valueOf(this.getVotes()) + ", missionVotingCompleted=" + this.isMissionVotingCompleted() + ", missionResult=" + this.getMissionResult() + ", missionFailCount=" + this.getMissionFailCount() + ", missionHistory=" + String.valueOf(this.getMissionHistory()) + ", lakeLadyHolderSeat=" + String.valueOf(this.getLakeLadyHolderSeat()) + ", lakeLadyUsedSeats=" + String.valueOf(this.getLakeLadyUsedSeats()) + ", lakeLadyInspectionHistory=" + String.valueOf(this.getLakeLadyInspectionHistory()) + ", lakeLadyInspectionResult=" + String.valueOf(this.getLakeLadyInspectionResult()) + ", lakeLadyPublicResult=" + String.valueOf(this.getLakeLadyPublicResult()) + ", lakeLadyLastInspectorSeat=" + String.valueOf(this.getLakeLadyLastInspectorSeat()) + ")";
    }

    @Generated
    public GameRoom() {
    }
}

