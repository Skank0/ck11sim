package com.ntcees.websocketdemo.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SignalDataList {

    private DataWrapper data;
    public String type = "ru.monitel.ck11.measurement-values.data.v2";
    public String time = Instant.now().toString();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }



    public SignalDataList() {
        this.data = new DataWrapper();
    }

    public DataWrapper getData() {
        return data;
    }

    public void setData(DataWrapper data) {
        this.data = data;
    }

    // Вложенный класс
    public static class DataWrapper {
        private List<SignalData> data = new ArrayList<>();

        public List<SignalData> getData() {
            return data;
        }

        public void setData(List<SignalData> data) {
            this.data = data;
        }
    }

}
