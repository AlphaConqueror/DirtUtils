package gg.dirtcraft.dirtutils.utils;

import org.bukkit.Location;

import java.util.UUID;

public class ChestData {

    private final UUID uuid;

    private final Location location;
    private Long millis = System.currentTimeMillis();
    private boolean verified = false;

    public ChestData(final UUID uuid, final Location location) {
        this.uuid = uuid;
        this.location = location;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public Location getLocation() {
        return this.location;
    }

    public Long getMillis() {
        return this.millis;
    }

    public void updateMillis() {
        this.millis = System.currentTimeMillis();
    }

    public boolean isVerified() {
        return this.verified;
    }

    public void verify() {
        this.verified = true;
    }

    public boolean isValid() {
        return System.currentTimeMillis() - this.millis < 1500;
    }
}
