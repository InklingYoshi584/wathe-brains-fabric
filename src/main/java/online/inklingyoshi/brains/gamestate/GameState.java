package online.inklingyoshi.brains.gamestate;

/**
 * GameState structure sent to DeepSeek LLM for decision making.
 */
public class GameState {
    public SelfInfo self;
    public java.util.List<PlayerInfo> players;
    public java.util.List<BodyInfo> bodiesSeen;
    public java.util.Map<String, String> peopleWhoSpokeToMe;
    public String trigger;
    public String memorySummary;

    public static class SelfInfo {
        public String name;
        public String role;
        public double health;
        public double coins;
        public String car;
        public String region;
        public Position position;
        public Double mood;
        public String moodPrompt;
        public String roomId;
        public Boolean inOwnRoom;
        public PsychoStatus psycho;
        public InventoryInfo inventory;
    }

    public static class Position {
        public double x, y, z;
    }

    public static class PsychoStatus {
        public boolean active;
        public double remainingSec;
        public double cooldownSec;
    }

    public static class InventoryInfo {
        public boolean hasKnife;
        public boolean hasRevolver;
        public boolean hasGrenade;
        public boolean hasLockpick;
        public boolean hasPoisonVial;
        public boolean hasScorpion;
        public boolean hasBodyBag;
        public boolean hasBlackout;
    }

    public static class PlayerInfo {
        public String name;
        public boolean isSelf;
        public boolean alive;
        public Double distance;
        public String car;
        public String region;
        public boolean inLineOfSight;
        public boolean armed;
        public Double suspiciousScore;
    }

    public static class BodyInfo {
        public String victim;
        public String car;
        public double timeFirstSeenSec;
        public boolean stillPresent;
    }

    public static class SpokeToMeInfo {
        public String name;
        public String lastMessage;
        public double timeLastSpokeSec;
    }
}
