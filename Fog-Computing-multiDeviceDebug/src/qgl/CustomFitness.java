package qgl;
import ec.multiobjective.MultiObjectiveFitness;

public class CustomFitness extends MultiObjectiveFitness {
    private double[] makespan; // 存储原始 makespan

    public void setMakespan(double[] makespan) {
        this.makespan = makespan;
    }

    public double[] getMakespan() {
        return this.makespan;
    }
}
