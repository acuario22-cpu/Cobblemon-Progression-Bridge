package com.hisroyalty.cobbledgacha.config;
public enum MachineType {
    GENERIC, SPECIFIC, SPAWNER;
    public static MachineType parse(String value) {
        if (value == null) return GENERIC;
        try { return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException e) { return GENERIC; }
    }
}
