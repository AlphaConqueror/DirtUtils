package gg.dirtcraft.dirtutils.utils;

import java.util.UUID;

public class VanishData {

    private final UUID uuid;
    private boolean hasIOption;
    private boolean hasBOption;

    public VanishData(final UUID uuid, final boolean hasIOption, final boolean hasBOption) {
        this.uuid = uuid;
        this.hasIOption = hasIOption;
        this.hasBOption = hasBOption;
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

    public boolean hasBOption() {
        return this.hasBOption;
    }

    public void setBOption(final boolean hasBOption) {
        this.hasBOption = hasBOption;
    }
}
