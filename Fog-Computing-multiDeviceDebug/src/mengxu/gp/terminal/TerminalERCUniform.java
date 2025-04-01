package mengxu.gp.terminal;

import ec.EvolutionState;
import ec.gp.ERC;
import ec.gp.GPFunctionSet;
import ec.gp.GPNode;
import ec.gp.GPTree;
import ec.util.Parameter;
import mengxu.gp.GPRuleEvolutionState;
import mengxu.rule.RuleType;

/**
 * The terminal ERC, with uniform selection.
 *
 * @author yimei
 */

public class TerminalERCUniform extends TerminalERC implements AttributeProvider{
    private RuleType ruleType;  // 存储规则类型 (SEQUENCING 或 ROUTING)

    public void setRuleType(RuleType ruleType) {
        this.ruleType = ruleType;
    }
    @Override
    public JobShopAttribute getJobShopAttribute() {
        if (terminal instanceof AttributeProvider) {
            return ((AttributeProvider) terminal).getJobShopAttribute();
        }
        return null;  // 如果 `terminal` 不是 `AttributeGPNode`，返回 `null`
    }
    @Override
    public void setup(final EvolutionState state, final Parameter base) {
        super.setup(state, base);

        //Assume here we are dealing with simple gp
        int subPopNum = 0;
        if (base.toString().endsWith("1")) {
            subPopNum = 1;
        }
        terminal = ((GPRuleEvolutionState)state).pickTerminalRandom(subPopNum);

    }

    @Override
    public void resetNode(EvolutionState state, int thread) {
        int subPopNum = 0;
        //buyong
        terminal = ((GPRuleEvolutionState)state).pickTerminalRandom(subPopNum);

        if (terminal instanceof ERC) {
            ERC ercTerminal = new DoubleERC();
            ercTerminal.resetNode(state, thread);
            terminal = ercTerminal;
        }
    }

    //============================use this one=================================

    public void resetNode(EvolutionState state, int thread, GPFunctionSet set,RuleType ruleType) {
        //Assume here we are dealing with simple gp

        int subPopNum = 0;
        if (set.toString().endsWith("1")) {
            subPopNum = 1;
        }

        // ✅ **调用 `pickTerminalRandom()`，确保使用正确的 `RuleType`**
        terminal = ((GPRuleEvolutionState) state).pickTerminalRandom(subPopNum, ruleType);

        // 如果是 ERC（进化常量节点），则重新初始化
        if (terminal instanceof ERC) {
            ERC ercTerminal = new DoubleERC();
            ercTerminal.resetNode(state, thread, set,ruleType);
            terminal = ercTerminal;
        }
//        // ✅ **使用 AP 机制选择终端**
//        terminal = ((GPRuleEvolutionState) state).pickTerminalRandom(subPopNum, ruleType);
//
//        //fzhang 2019.5.27 another terminal with different parameters
//        //terminal = ((GPRuleEvolutionState)state).pickTerminalRandom(state, subPopNum);
//
//        if (terminal instanceof ERC) {
//            ERC ercTerminal = new DoubleERC();
//            ercTerminal.resetNode(state, thread, set);
//            terminal = ercTerminal;
//        }
    }
    //mutateERC will call this method
    @Override
    public void resetNode(EvolutionState state, int thread, GPFunctionSet set) {
        //Assume here we are dealing with simple gp

        int subPopNum = 0;
        if (set.toString().endsWith("1")) {
            subPopNum = 1;
        }
        System.out.println("resetNode1000没规则");
        // ✅ **使用 AP 机制选择终端**
       // terminal = ((GPRuleEvolutionState) state).pickTerminalRandom(subPopNum, ruleType);
        terminal = ((GPRuleEvolutionState)state).pickTerminalRandom(subPopNum);
        //fzhang 2019.5.27 another terminal with different parameters
        //terminal = ((GPRuleEvolutionState)state).pickTerminalRandom(state, subPopNum);

        if (terminal instanceof ERC) {
            ERC ercTerminal = new DoubleERC();
            ercTerminal.resetNode(state, thread, set);
            terminal = ercTerminal;
        }
    }

    @Override
    public void mutateERC(EvolutionState state, int thread, GPFunctionSet set) {
        resetNode(state, thread, set);
    }
}
