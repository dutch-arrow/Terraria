package nl.das.terraria.json;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TemperatureRule {

    @SerializedName("active")
    @Expose
    private String active;
    @SerializedName("from")
    @Expose
    private String from;
    @SerializedName("to")
    @Expose
    private String to;
    @SerializedName("temp_ideal")
    @Expose
    private Integer tempIdeal;
    @SerializedName("temp_threshold")
    @Expose
    private Integer tempThreshold;
    @SerializedName("delay")
    @Expose
    private Integer delay;
    @SerializedName("actions")
    @Expose
    private List<Action> actions = null;

    public String getActive() {
        return active;
    }

    public void setActive(String active) {
        this.active = active;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public Integer getTempIdeal() {
        return tempIdeal;
    }

    public void setTempIdeal(Integer tempIdeal) {
        this.tempIdeal = tempIdeal;
    }

    public Integer getTempThreshold() {
        return tempThreshold;
    }

    public void setTempThreshold(Integer tempThreshold) {
        this.tempThreshold = tempThreshold;
    }

    public Integer getDelay() {
        return delay;
    }

    public void setDelay(Integer delay) {
        this.delay = delay;
    }

    public List<Action> getActions() {
        return actions;
    }

    public void setActions(List<Action> actions) {
        this.actions = actions;
    }

}