package com.ozgen.jraft.util;

import java.util.Random;

public class ThreadUtils {
    private static final Random RANDOM = new Random();

    public static void addRandomDelayMilliseconds(){
        int randomMilliseconds = 100 + RANDOM.nextInt(401); // 401 because 500 - 100 + 1 = 401
        try {
            Thread.sleep(randomMilliseconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
