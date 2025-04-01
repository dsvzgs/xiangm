package mengxu.gp.terminal;

import ec.EvolutionState;
import ec.Problem;
import ec.gp.ADFStack;
import ec.gp.GPData;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import mengxu.gp.CalcPriorityProblem;
import mengxu.gp.data.DoubleData;

/**
 * The job shop attribute as terminal.
 *
 * @author yimei
 */

public class AttributeGPNode extends GPNode implements AttributeProvider{

    private final JobShopAttribute attribute;
    private double activationProbability;
    public AttributeGPNode(JobShopAttribute attribute) {
        super();
        children = new GPNode[0];
        this.attribute = attribute;
        this.activationProbability = JobShopAttribute.getActivationProbability(attribute);
        System.out.println("AttributeGPNode使用了没规则" );
    }

    public JobShopAttribute getJobShopAttribute() {
        return attribute;
    }
    // ✅ 添加 setActivationProbability 方法
    public void setActivationProbability(double probability) {
        this.activationProbability = probability;
    }

    public double getActivationProbability() {
        return activationProbability;
    }
    @Override
    public String toString() {
        return attribute.getName();
    }

    @Override
    public int expectedChildren() {
        return 0;
    }

    @Override
    public void eval(EvolutionState state, int thread, GPData input,
                     ADFStack stack, GPIndividual individual, Problem problem) {
        // The problem is essentially a priority calculation.
        CalcPriorityProblem calcPrioProb = ((CalcPriorityProblem)problem);

        DoubleData data = ((DoubleData)input);
        if(calcPrioProb.getTaskOption() == null || calcPrioProb.getSystemState() == null) {
        	System.out.println("null");
        }
        
        data.value = attribute.value(
                calcPrioProb.getTaskOption(),
                calcPrioProb.getServer(),
                calcPrioProb.getSystemState());
    }

    @Override
    public int hashCode() {
        return attribute.getName().hashCode();
    }

    public boolean equals(Object other) {
        if (other instanceof AttributeGPNode) {
            AttributeGPNode o = (AttributeGPNode)other;
            return (attribute == o.attribute);
        }

        return false;
    }
}
