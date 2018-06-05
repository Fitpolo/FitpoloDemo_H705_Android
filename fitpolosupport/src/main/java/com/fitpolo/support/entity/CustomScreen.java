package com.fitpolo.support.entity;

public class CustomScreen {
    public boolean duration;
    public boolean calorie;
    public boolean distance;
    public boolean heartrate;
    public boolean step;
    public boolean sleep;

    public CustomScreen(boolean duration, boolean calorie, boolean distance, boolean heartrate, boolean step, boolean sleep) {
        this.duration = duration;
        this.calorie = calorie;
        this.distance = distance;
        this.heartrate = heartrate;
        this.step = step;
        this.sleep = sleep;
    }

    @Override
    public String toString() {
        return "CustomScreen{" +
                "duration=" + duration +
                ", calorie=" + calorie +
                ", distance=" + distance +
                ", heartrate=" + heartrate +
                ", step=" + step +
                ", sleep=" + sleep +
                '}';
    }
}
