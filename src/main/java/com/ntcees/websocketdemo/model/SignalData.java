package com.ntcees.websocketdemo.model;

public class SignalData {
    private String id;
    private double value;
    private long timestamp;

    // Конструкторы
    public SignalData() {}

    public SignalData(String id, double value) {
        this.id = id;
        this.value = value;
        this.timestamp = System.currentTimeMillis();
    }

    // Геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
