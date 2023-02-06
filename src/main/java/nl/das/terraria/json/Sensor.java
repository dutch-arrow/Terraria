package nl.das.terraria.json;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Sensor {

    @SerializedName("location")
    @Expose
    private String location;
    @SerializedName("temperature")
    @Expose
    private Integer temperature;
    @SerializedName("humidity")
    @Expose
    private Integer humidity;

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getTemperature() {
        return temperature;
    }

    public void setTemperature(Integer temperature) {
        this.temperature = temperature;
    }

    public Integer getHumidity() { return humidity; }

    public void setHumidity(Integer humidity) { this.humidity = humidity; }

}