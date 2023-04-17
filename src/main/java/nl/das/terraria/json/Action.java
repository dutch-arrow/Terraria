package nl.das.terraria.json;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Action {

    @SerializedName("device")
    @Expose
    private String device;
    @SerializedName("on_period")
    @Expose
    private Integer onPeriod;

    public Action(String dev, Integer period) {
        this.device = dev;
        this.onPeriod = period;
    }
    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public Integer getOnPeriod() {
        return onPeriod;
    }

    public void setOnPeriod(Integer onPeriod) {
        this.onPeriod = onPeriod;
    }

}