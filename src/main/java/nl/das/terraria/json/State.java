package nl.das.terraria.json;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class State {

    @SerializedName("device")
    @Expose
    private String device;
    @SerializedName("state")
    @Expose
    private String state;
    @SerializedName("end_time")
    @Expose
    private String endTime;
    @SerializedName("hours_on")
    @Expose
    private Integer hoursOn;
    @SerializedName("manual")
    @Expose
    private String manual;

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Integer getHoursOn() {
        return hoursOn;
    }

    public void setHoursOn(Integer hoursOn) {
        this.hoursOn = hoursOn;
    }

    public String getManual() {
        return manual;
    }

    public void setManual(String manual) {
        this.manual = manual;
    }

}