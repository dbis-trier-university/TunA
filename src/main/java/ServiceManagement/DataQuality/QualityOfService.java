package ServiceManagement.DataQuality;

import Configuration.Utils.UserPreferences;

import java.util.List;

public class QualityOfService {
    private int execTime, calls;
    private double coverage, reliability;

    // *****************************************************************************************************************
    // Constructor
    // *****************************************************************************************************************

    public QualityOfService(int calls, int execTime, double coverage, double reliability) {
        this.calls = calls;
        this.execTime = execTime;
        this.coverage = coverage;
        this.reliability = reliability;
    }

    // *****************************************************************************************************************
    // Public Methods
    // *****************************************************************************************************************

    public double get(String key){
        if(key.equalsIgnoreCase("Time")) return getExecTime();
        else if(key.equalsIgnoreCase("Coverage")) return getCoverage();
        else return getReliability();
    }

    public int getExecTime() {
        return execTime;
    }

    public int getCalls() {
        return calls;
    }

    public double getCoverage() {
        return coverage;
    }

    public double getReliability() {
        return reliability;
    }

    public boolean fulfillsUserSettings(UserPreferences setting){
        if(this.execTime > setting.get("Time")) return false;

        // TODO
//        if(this.coverage < setting.get("Coverage")) return false;
//        if(this.reliability < setting.get("Reliability")) return false;

        return true;
    }

    @Override
    public String toString(){
        return "(" + this.calls + ", " + this.execTime + ", " + this.coverage + ", " + this.reliability + ")";
    }

    public static int compareQuality(QualityOfService q1, QualityOfService q2, UserPreferences userPreferences){
        List<String> oPref = userPreferences.getOrderedPreferences();

        if(q1.get(oPref.get(0)) == q2.get(oPref.get(0))){
            if(q1.get(oPref.get(1)) == q2.get(oPref.get(1))){
                int c = Double.compare(q1.get(oPref.get(2)),q2.get(oPref.get(2)));
                return c;
            } else {
                int c = Double.compare(q1.get(oPref.get(1)),q2.get(oPref.get(1)));
                return c;
            }
        } else {
            int c = Double.compare(q1.get(oPref.get(0)),q2.get(oPref.get(0)));
            return c;
        }
    }
}
