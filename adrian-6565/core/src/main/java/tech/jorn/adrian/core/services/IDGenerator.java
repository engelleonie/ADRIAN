package tech.jorn.adrian.core.services;

import java.util.UUID;

public class IDGenerator {
    static IDGenerator instance = new IDGenerator();
    public static IDGenerator getInstance() {
        return instance;
    }

    public String getID() {
        return UUID.randomUUID().toString();
    }
}
