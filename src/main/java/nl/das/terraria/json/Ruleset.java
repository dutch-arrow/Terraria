package nl.das.terraria.json;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Ruleset {

    @SerializedName("terrarium")
    @Expose
    private Integer terrarium;
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
    @SerializedName("rules")
    @Expose
    private List<Rule> rules = null;

    public Integer getTerrarium() {
        return terrarium;
    }

    public void setTerrarium(Integer terrarium) {
        this.terrarium = terrarium;
    }

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

    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }

}