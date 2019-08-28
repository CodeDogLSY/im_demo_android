package com.lsy.imdemo;

import java.util.UUID;

public class StrUtil {
    public static String getRandomName() {
        long currentTime = System.currentTimeMillis();
        String name = String.valueOf(currentTime);
        name = name.substring(7);
        return "guide" + name;
    }

    public static String getRandomId() {
        UUID id = UUID.randomUUID();
        return id.toString();
    }
}
