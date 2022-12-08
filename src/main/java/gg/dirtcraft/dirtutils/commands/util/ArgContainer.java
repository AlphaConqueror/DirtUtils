package gg.dirtcraft.dirtutils.commands.util;

public class ArgContainer {

    private final ArgType argType;
    private final String label;
    private final boolean isOptional;

    public ArgContainer(final ArgType argType, final String label, final boolean isOptional) {
        this.argType = argType;
        this.label = label;
        this.isOptional = isOptional;
    }

    public ArgType getArgType() {
        return this.argType;
    }

    public String getLabel() {
        return this.label;
    }

    public boolean isOptional() {
        return this.isOptional;
    }

    @Override
    public String toString() {
        final char lBracket = this.isOptional ? '[' : '<';
        final char rBracket = this.isOptional ? ']' : '>';

        return String.format("%c%s: %s%c", lBracket, this.argType.getName(), this.label, rBracket);
    }
}
