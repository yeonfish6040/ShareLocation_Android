package com.yeonfish.sharelocation.util;

public class UUID {
    public static String generate() {
        String uuid = java.util.UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
        return uuid;
    }
}
