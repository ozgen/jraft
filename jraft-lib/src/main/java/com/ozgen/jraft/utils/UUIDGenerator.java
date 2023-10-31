package com.ozgen.jraft.utils;

import java.util.UUID;

public class UUIDGenerator {

    public static String generateNodeId() {
        return UUID.randomUUID().toString();
    }
}
