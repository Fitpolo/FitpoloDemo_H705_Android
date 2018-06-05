package com.fitpolo.support.entity;

import java.io.Serializable;

public enum OrderType implements Serializable {
    READ_CHARACTER("READ_CHARACTER", "0000ffb0-0000-1000-8000-00805f9b34fb"),
    WRITE_CHARACTER("WRITE_CHARACTER", "0000ffb1-0000-1000-8000-00805f9b34fb"),
    STEP_CHARACTER("STEP_CHARACTER", "0000ffb2-0000-1000-8000-00805f9b34fb"),
    HEART_RATE_CHARACTER("HEART_RATE_CHARACTER", "0000ffb3-0000-1000-8000-00805f9b34fb"),
    ;


    private String uuid;
    private String name;

    OrderType(String name, String uuid) {
        this.name = name;
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }
}
