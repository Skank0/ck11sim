package com.ntcees.websocketdemo.model;

import java.util.ArrayList;
import java.util.List;

public class SignalDataList {
    private List<SignalData> value = new ArrayList<>();

    public List<SignalData> getValue() {
        return value;
    }

    public void setValue(List<SignalData> value) {
        this.value = value;
    }
}
