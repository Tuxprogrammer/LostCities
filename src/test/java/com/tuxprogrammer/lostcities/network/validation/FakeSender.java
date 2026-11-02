package com.tuxprogrammer.lostcities.network.validation;

/** A {@link Sender} for tests: a player at a position, in a dimension, with a permission level. */
public final class FakeSender implements Sender {

    public String name = "TestPlayer";
    public boolean alive = true;
    public int dimension;
    public double x;
    public double y = 64;
    public double z;
    public int permissionLevel;

    /** Returns a living, non-operator player at (0, 64, 0) in the overworld. */
    public static FakeSender player() {
        return new FakeSender();
    }

    /** Returns a living operator (level 4, like a dedicated server's default) at (0, 64, 0) in the overworld. */
    public static FakeSender operator() {
        FakeSender sender = new FakeSender();
        sender.permissionLevel = 4;
        return sender;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean isAlive() {
        return alive;
    }

    @Override
    public int dimensionId() {
        return dimension;
    }

    @Override
    public double x() {
        return x;
    }

    @Override
    public double y() {
        return y;
    }

    @Override
    public double z() {
        return z;
    }

    @Override
    public boolean hasPermissionLevel(int level) {
        return permissionLevel >= level;
    }
}
