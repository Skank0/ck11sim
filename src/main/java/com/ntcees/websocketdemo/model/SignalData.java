package com.ntcees.websocketdemo.model;

import java.time.Instant;

public class SignalData {
    private String uid;
    private String timeStamp;
    private String timeStamp2;
    private long qCode;
    private double value;

    // Конструкторы
    public SignalData() {}

    public SignalData(String uid, double value) {
        this.uid = uid;
        this.value = value;
        this.timeStamp = Instant.now().toString();
        this.timeStamp2 = Instant.now().toString();
        this.qCode = 2;
    }

    // Геттеры и сеттеры
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
    public String getTimeStamp() { return timeStamp; }
    public void setTimeStamp(String timeStamp) { this.timeStamp = timeStamp; }

    @Override
    public String toString() {
        return "SignalData[" + uid + "]=" + value + " t=" + timeStamp;
    }


    public long getqCode() {
        return qCode;
    }
    public String getTimeStamp2() {
        return timeStamp2;
    }

    public void setqCode(long qCode) {
        this.qCode = qCode;
    }

    public void setTimeStamp2(String timeStamp2) {
        this.timeStamp2 = timeStamp2;
    }
}
