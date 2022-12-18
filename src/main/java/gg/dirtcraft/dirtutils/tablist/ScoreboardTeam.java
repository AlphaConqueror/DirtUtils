package gg.dirtcraft.dirtutils.tablist;

public class ScoreboardTeam {

    private final String name;
    private final String prefix;

    public ScoreboardTeam(final String name, final String prefix) {
        this.name = name;
        this.prefix = prefix;
    }

    public String getName() {
        return this.name;
    }

    public String getPrefix() {
        return this.prefix;
    }
}
