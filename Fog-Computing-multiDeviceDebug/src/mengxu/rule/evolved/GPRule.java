package mengxu.rule.evolved;

import ec.gp.GPNode;
import ec.gp.GPTree;
import mengxu.feature.ignore.Ignorer;
import mengxu.gp.CalcPriorityProblem;
import mengxu.gp.GPNodeComparator;
import mengxu.gp.data.DoubleData;
import mengxu.gp.terminal.AttributeGPNode;
import mengxu.gp.terminal.AttributeProvider;
import mengxu.gp.terminal.JobShopAttribute;
import mengxu.gp.terminal.TerminalERCUniform;
import mengxu.rule.AbstractRule;
import mengxu.rule.RuleType;
import mengxu.simulation.state.SystemState;
import mengxu.taskscheduling.Server;
import mengxu.taskscheduling.TaskOption;
import mengxu.util.lisp.LispParser;

import java.util.ArrayList;
import java.util.List;

/**
 * The GP-evolved rule.
 * <p>
 * Created by YiMei on 27/09/16.
 */
public class GPRule extends AbstractRule {

    private GPTree gpTree;
    private String lispString;

    public GPRule(RuleType t, GPTree gpTree) {
        name = "\"GPRule\"";
        this.gpTree = gpTree;
        type = t;
    }

    public GPRule(RuleType t, GPTree gpTree, String expression) {
        name = "\"GPRule\"";
        this.lispString = expression;
        this.gpTree = gpTree;
        this.type = t;
    }

    public GPTree getGPTree() {
        return gpTree;
    }

    public void setGPTree(GPTree gpTree) {
        this.gpTree = gpTree;
    }

    public String getLispString() {
        return lispString;
    }

    public static GPRule readFromLispExpression(RuleType type, String expression) {
        GPTree tree = LispParser.parseJobShopRule(expression);

        return new GPRule(type, tree, expression);
    }

    public void ignore(GPNode tree, GPNode feature, Ignorer ignorer) {
    	
    	//System.out.println(tree.depth());
        //System.out.println(feature.depth());
        
        if (tree.depth() < feature.depth())       	
            return;

        if (GPNodeComparator.equals(tree, feature)) {
            ignorer.ignore(tree);

            return;
        }

        if (tree.depth() == feature.depth())
            return;  //after ignoring, check again

        for (GPNode child : tree.children) {
            ignore(child, feature, ignorer);
        }
    }

    public void ignore(GPNode feature, Ignorer ignorer) {
        ignore(gpTree.child, feature, ignorer);
    }
    public List<JobShopAttribute> getAttributes() {
        List<JobShopAttribute> attributes = new ArrayList<>();

        // ✅ 先检查 gpTree 是否为空
        if (gpTree == null || gpTree.child == null) {
            System.err.println("Error: GPRule's GP tree is null or has no child.");
            return attributes;
        }

        // ✅ 递归遍历 GP 树
        traverseTreeForAttributes(gpTree.child, attributes);

        if (attributes.isEmpty()) {
            System.err.println("Warning: No JobShopAttribute found in GP tree.");
        }

        return attributes;
    }
    private void traverseTreeForAttributes(GPNode node, List<JobShopAttribute> attributes) {
        if (node == null) return;

       // System.out.println("当前节点类名: " + node.getClass().getName());

        if (node instanceof AttributeProvider) {  // ✅ 统一检查 AttributeGPNode 和 TerminalERCUniform
            JobShopAttribute attr = ((AttributeProvider) node).getJobShopAttribute();
            if (attr != null && !attributes.contains(attr)) {
               // System.out.println("✅ 解析出的 JobShopAttribute: " + attr.toString());
                attributes.add(attr);
            }
        }

        for (GPNode child : node.children) {
            traverseTreeForAttributes(child, attributes);
        }
    }

    private void traverseTreeForAttributes2(GPNode node, List<JobShopAttribute> attributes) {
        if (node == null) return;  // ✅ 避免空指针异常
        // 调试信息
        String nodeClassName = node.getClass().getName();
        System.out.println("当前节点类名: " + nodeClassName);
        if (node instanceof AttributeGPNode) {
            JobShopAttribute attr = ((AttributeGPNode) node).getJobShopAttribute();
            if (!attributes.contains(attr)) {
                System.out.println("添加的节点是 " + attr.toString());
                attributes.add(attr);
            }
        }

        // ✅ 递归遍历所有子节点
        for (GPNode child : node.children) {
            traverseTreeForAttributes(child, attributes);
        }
    }

    public void applyPriorityBoost(GPRule rule, RuleType ruleType) {
        for (GPNode node : rule.getGPTree().child.children) {
            if (node instanceof AttributeGPNode) {
                AttributeGPNode attrNode = (AttributeGPNode) node;
                JobShopAttribute attr = attrNode.getJobShopAttribute();

                if (ruleType == RuleType.SEQUENCING && (attr == JobShopAttribute.PROC_TIME || attr == JobShopAttribute.DOWNLOAD_TIME)) {
                    attrNode.setActivationProbability(1.0);  // ✅ 强制 PT 和 DT 为最重要
                }

                if (ruleType == RuleType.ROUTING && attr == JobShopAttribute.NUM_TASK_REMAINING) {
                    attrNode.setActivationProbability(1.0);  // ✅ 强制 NTR 为最重要
                }
            }
        }
    }

    public double priority(TaskOption taskOption, Server server,
                           SystemState systemState) {
        CalcPriorityProblem calcPrioProb =
                new CalcPriorityProblem(taskOption, server, systemState);

        DoubleData tmp = new DoubleData();
        gpTree.child.eval(null, 0, tmp, null, null, calcPrioProb);

        return tmp.value;
    }
}
