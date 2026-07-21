package com.tradechoice.client.platform;

public final class Platforms {

    private static volatile Platform active;

    private Platforms() {
    }

    public static void set(Platform impl) {
        if (active != null) {
            throw new IllegalStateException("Platform already set: " + active.loaderName());
        }
        active = impl;
    }

    public static Platform get() {
        Platform p = active;
        if (p == null) {
            throw new IllegalStateException("Platform not yet bootstrapped");
        }
        return p;
    }
}
