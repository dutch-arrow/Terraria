package nl.das.terraria.json;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Timer {

    @SerializedName("device")
    @Expose
    private String device;
    @SerializedName("index")
    @Expose
    private Integer index;
    @SerializedName("hour_on")
    @Expose
    private Integer hourOn;
    @SerializedName("minute_on")
    @Expose
    private Integer minuteOn;
    @SerializedName("hour_off")
    @Expose
    private Integer hourOff;
    @SerializedName("minute_off")
    @Expose
    private Integer minuteOff;
    @SerializedName("repeat")
    @Expose
    private Integer repeat;
    @SerializedName("period")
    @Expose
    private Integer period;

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public Integer getHourOn() {
        return hourOn;
    }

    public void setHourOn(Integer hourOn) {
        this.hourOn = hourOn;
    }

    public Integer getMinuteOn() {
        return minuteOn;
    }

    public void setMinuteOn(Integer minuteOn) {
        this.minuteOn = minuteOn;
    }

    public Integer getHourOff() {
        return hourOff;
    }

    public void setHourOff(Integer hourOff) {
        this.hourOff = hourOff;
    }

    public Integer getMinuteOff() {
        return minuteOff;
    }

    public void setMinuteOff(Integer minuteOff) {
        this.minuteOff = minuteOff;
    }

    public Integer getRepeat() {
        return repeat;
    }

    public void setRepeat(Integer repeat) {
        this.repeat = repeat;
    }

    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }

}