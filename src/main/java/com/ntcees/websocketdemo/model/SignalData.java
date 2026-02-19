package com.ntcees.websocketdemo.model;

import java.time.Instant;

public class SignalData {
    private String uid;
    private String timestamp;
    private String timestamp2;
    private long qCode;
    private double value;

    // Конструкторы
    public SignalData() {}

    public SignalData(String uid, double value) {
        this.uid = uid;
        this.value = value;
        this.timestamp = Instant.now().toString();
        this.timestamp2 = Instant.now().toString();
        this.qCode = 1;
    }

    // Геттеры и сеттеры
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "SignalData[" + uid + "]=" + value + " t=" + timestamp;
    }


    public long getqCode() {
        return qCode;
    }
    public String getTimestamp2() {
        return timestamp2;
    }

    public void setqCode(long qCode) {
        this.qCode = qCode;
    }

    public void setTimestamp2(String timestamp2) {
        this.timestamp2 = timestamp2;
    }
}
