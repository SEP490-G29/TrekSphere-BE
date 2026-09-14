package com.sep.treksphere.matching.enums;

public enum IncidentType {
    INJURY("chấn thương"),
    LOST("lạc đường"),
    WEATHER("thời tiết xấu"),
    SUPPLIES("thiếu vật tư"),
    OTHER("sự cố khác");

    private final String label;

    IncidentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
