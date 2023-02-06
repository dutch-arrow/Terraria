package nl.das.terraria.json;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class States {
    @SerializedName("trace")
    @Expose
    private String trace;
    @SerializedName("state")
    @Expose
    private List<State> states;

    public String getTrace() {
        return trace;
    }

    public void setTrace(String trace) {
        this.trace = trace;
    }

    public List<State> getStates() {
        return states;
    }

    public void setStates(List<State> states) {
        this.states = states;
    }
}
