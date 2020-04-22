package eu.stamp.botsing.ga.stoppingconditions;

public class MaxTimeStoppingCondition extends org.evosuite.ga.stoppingconditions.MaxTimeStoppingCondition {

    public long getStartingTime(){
        return this.startTime;
    }
}
