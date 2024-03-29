package nl.das.terraria.json;

import android.bluetooth.BluetoothDevice;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Properties {

    @SerializedName("nr_of_timers")
    @Expose
    private Integer nrOfTimers;
    @SerializedName("nr_of_programs")
    @Expose
    private Integer nrOfPrograms;
    @SerializedName("devices")
    @Expose
    private List<Device> devices = null;

    private transient String tcuName;
    private transient String deviceName;
    private transient String uuid;
    private transient String ip;
    private transient BluetoothDevice device;
    private transient String mockPostfix;

    public BluetoothDevice getDevice() { return device; }

    public void setDevice(BluetoothDevice device) { this.device = device; }

    public Integer getNrOfTimers() {
        return nrOfTimers;
    }

    public void setNrOfTimers(Integer nrOfTimers) {
        this.nrOfTimers = nrOfTimers;
    }

    public Integer getNrOfPrograms() {
        return nrOfPrograms;
    }

    public void setNrOfPrograms(Integer nrOfPrograms) {
        this.nrOfPrograms = nrOfPrograms;
    }

    public List<Device> getDevices() {
        return devices;
    }

    public void setDevices(List<Device> devices) {
        this.devices = devices;
    }

    public String getMockPostfix() { return mockPostfix; }

    public void setTcuName(String tcuName) { this.tcuName = tcuName;}

    public String getTcuName() { return tcuName; }

    public void setMockPostfix(String mockPostfix) { this.mockPostfix = mockPostfix; }

    public String getDeviceName() { return deviceName; }

    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getUuid() { return uuid; }

    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getIp() { return ip; }

    public void setIp(String ip) { this.ip = ip; }
}