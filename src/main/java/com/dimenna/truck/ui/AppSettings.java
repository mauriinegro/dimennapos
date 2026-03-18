package com.dimenna.truck.ui;

import java.util.prefs.Preferences;

public final class AppSettings {
    private static final Preferences PREFS = Preferences.userRoot().node("dimenna-pos");

    private AppSettings() {}

    public static void setSelectedPrinter(String name) {
        PREFS.put("printer", name == null ? "" : name);
    }

    public static String getSelectedPrinter() {
        return PREFS.get("printer", "");
    }
}
