package gg.dirtcraft.dirtutils.commands.core.parser;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public abstract class ArgContainer<T> {

    private static long nextId = 0;
    private final long id;
    private final String label;
    private final boolean isOptional;

    protected ArgContainer(final String label, final boolean isOptional) {
        this.id = nextId;
        this.label = label;
        this.isOptional = isOptional;

        nextId++;
    }

    public abstract T parse(final List<String> args);

    protected abstract String getTypeName();

    public long getId() {
        return this.id;
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

        return String.format("%c%s%s%s%c", lBracket, this.getTypeName(), (this.getTypeName().isEmpty() ? "" : ": "),
                this.label, rBracket);
    }

    public static class PlayerArgContainer extends ArgContainer<Player> {

        public PlayerArgContainer(final String label, final boolean isOptional) {
            super(label, isOptional);
        }

        @Override
        public Player parse(final List<String> args) {
            if (args.size() == 0) {
                return null;
            }

            final Player player = Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.getName().startsWith(args.get(0))).findFirst().orElse(null);

            args.remove(0);

            return player;
        }

        @Override
        protected String getTypeName() {
            return "Player";
        }
    }

    public static class OptionArgContainer extends ArgContainer<Boolean> {

        public OptionArgContainer(final String label) {
            super(label, true);
            assert label.startsWith("-") : "Option does not start with '-'.";
        }

        @Override
        public Boolean parse(final List<String> args) {
            boolean foundOption = false;

            // make copy of list to avoid concurrent modification exception
            for (final String arg : new ArrayList<>(args)) {
                if (arg.equals(this.getLabel())) {
                    foundOption = true;
                    args.remove(arg);
                }
            }

            return foundOption;
        }

        @Override
        protected String getTypeName() {
            return "";
        }
    }
}
