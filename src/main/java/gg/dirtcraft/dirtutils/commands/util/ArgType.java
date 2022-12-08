package gg.dirtcraft.dirtutils.commands.util;

public enum ArgType {
    PLAYER("player"), WORD("word"), TEXT("text"), SHORT_NUMBER("number"), INT_NUMBER("number"), LONG_NUMBER("number"), DOUBLE("double");

    private final String name;

    ArgType(final String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
