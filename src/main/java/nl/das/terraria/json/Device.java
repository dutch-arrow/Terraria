package nl.das.terraria.json;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Device {

    @SerializedName("device")
    @Expose
    private String device;
    @SerializedName("nr_of_timers")
    @Expose
    private Integer nrOfTimers;
    @SerializedName("lc_counted")
    @Expose
    private boolean lifecycle;

    public String getDevice() { return device; }

    public void setDevice(String device) { this.device = device; }

    public Integer getNrOfTimers() { return nrOfTimers;}

    public void setNrOfTimers(Integer nrOfTimers) { this.nrOfTimers = nrOfTimers; }

    public boolean isLifecycle() { return lifecycle; }

    public void setLifecycle(boolean lifecycle) { this.lifecycle = lifecycle; }
}
