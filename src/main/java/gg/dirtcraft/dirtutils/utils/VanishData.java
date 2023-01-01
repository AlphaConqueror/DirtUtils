package gg.dirtcraft.dirtutils.utils;

import java.util.UUID;

public class VanishData {

    private final UUID uuid;
    private boolean hasIOption;

    public VanishData(final UUID uuid, final boolean hasIOption) {
        this.uuid = uuid;
        this.hasIOption = hasIOption;
    }

    public UUID getUniqueId() {
        return this.uuid;
    }

    public boolean hasIOption() {
        return this.hasIOption;
    }

    public void setIOption(final boolean hasIOption) {
        this.hasIOption = hasIOption;
    }
}
