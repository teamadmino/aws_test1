package com.teamadmino.application.fbzprod.utils;

public class Utils {

    public static String fmt52(int id) {
        return String.format("%05d/%02d", id / 100, id % 100);
    }

    public static String fmt62(int id) {
        return String.format("%06d/%02d", id / 100, id % 100);
    }

    public static String fmtDate(int dt) {
        return String.format("%04d-%02d-%02d", dt >> 16, (dt >> 8) & 0xFF, dt & 0xFF);
    }

}