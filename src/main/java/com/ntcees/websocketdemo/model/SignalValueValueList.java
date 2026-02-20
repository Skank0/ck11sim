package com.ntcees.websocketdemo.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SignalValueValueList {
    private List<DataWrapperValueValue> value;
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



    public SignalValueValueList() {
        this.value = new ArrayList<>();
    }

    public List<DataWrapperValueValue> getValue() {
        return value;
    }

    public void setValue(List<DataWrapperValueValue> value) {
        this.value = value;
    }

    // Вложенный класс
    public static class DataWrapperValueValue {
        private List<SignalData> value = new ArrayList<>();

        public List<SignalData> getValue() {
            return value;
        }

        public void setValue(List<SignalData> value) {
            this.value = value;
        }

        public DataWrapperValueValue(List<SignalData> value) {
            this.value = value;
        }
    }
}
