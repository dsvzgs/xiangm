package mengxu.gp;

import ec.gp.GPNode;
import ec.gp.GPTree;
import mengxu.gp.function.Div;
import mengxu.gp.function.Mul;
import mengxu.gp.terminal.AttributeGPNode;
import mengxu.gp.terminal.AttributeProvider;
import mengxu.gp.terminal.JobShopAttribute;

/**
 * Compare two GP nodes to see if they are equivalent.
 *
 * Created by YiMei on 5/10/16.
 */
public class GPNodeComparator {

    //qgl
    // 计算 GP 树的节点数量（用于 PF1 计算）
    public static int countNodes(GPNode node) {
        if (node == null) return 0;
        int count = 1;
        for (GPNode child : node.children) {
            count += countNodes(child);
        }
        return count;
    }
    // 计算 GP 树中的除法运算符数量（用于 PF2 计算）
    public static int countDivOperators(GPNode node) {
        if (node == null) return 0;
        int count = (node instanceof Div) ? 1 : 0;
        // System.out.println("node: " + node.toString() + ", count: " + count);
        for (GPNode child : node.children) {
            count += countDivOperators(child);
        }
        return count;
    }
    // 计算 GP 树中的所有运算符数量（用于 PF2 计算）
    public static int countTotalOperators(GPNode node) {
        if (node == null) return 0;
        int count = isOperator(node) ? 1 : 0;
        for (GPNode child : node.children) {
            count += countTotalOperators(child);
        }
        return count;
    }
    // 判断是否是运算符（+，-，*，/，max，min）
    private static boolean isOperator(GPNode node) {
        String symbol = node.toString();
        return symbol.equals("+") || symbol.equals("-") || symbol.equals("*") || symbol.equals("/") || symbol.equals("max") || symbol.equals("min");
    }
    // 计算 GP 树中的动态终端和静态终端数量（用于 PF3 计算）
    public static int[] countTerminalTypes(GPNode node) {
        if (node == null) return new int[]{0, 0}; // {numStatic, numDynamic}
        int numStatic = 0, numDynamic = 0;
        if (node instanceof AttributeProvider) {
            JobShopAttribute attr = ((AttributeProvider) node).getJobShopAttribute();
            if (attr.isStatic()) numStatic++;
            else if (attr.isDynamic()) numDynamic++;
        }
        for (GPNode child : node.children) {
            int[] childCounts = countTerminalTypes(child);
            numStatic += childCounts[0];
            numDynamic += childCounts[1];
        }
        return new int[]{numStatic, numDynamic};
    }
    public static boolean TreeEquals(GPTree t1, GPTree t2){
        GPNode node1 = t1.child;
        GPNode node2 = t2.child;

        return equals(node1, node2);
//        return equals(node1, node2);
    }

    public static boolean equals(GPNode o1, GPNode o2) {
        if (o1.toString().equals(o2.toString())) {
            if (o1.children.length == o2.children.length) {
                if (o1.children.length == 0)
                    return true;

                switch (o1.toString()) {
                    case "+":
                        return sameChildrenUnordered(o1.children, o2.children);
                    case "-":
                        return sameChildrenUnordered(o1.children, o2.children);
                    case "*":
                        return sameChildrenUnordered(o1.children, o2.children);
                    case "/":
                        return sameChildrenUnordered(o1.children, o2.children);
                    case "max":
                        return sameChildrenUnordered(o1.children, o2.children);
                    case "min":
                        return sameChildrenUnordered(o1.children, o2.children);
                    case "if":
                        return sameChildrenOrdered(o1.children, o2.children);
                }
            }
        }

        return false;
    }

    public static boolean sameChildrenOrdered(GPNode[] children1,
                                              GPNode[] children2) {
        for (int i = 0; i < children1.length; i++) {
            boolean same = equals(children1[i], children2[i]);

            if (!same)
                return false;
        }

        return true;
    }

    public static boolean sameChildrenUnordered(GPNode[] children1,
                                                GPNode[] children2) {
        boolean[] matched = new boolean[children2.length];

        for (int i = 0; i < children1.length; i++) {
            boolean foundSame = false;

            for (int j = 0; j < children2.length; j++) {
                if (matched[j])
                    continue;

                boolean same = equals(children1[i], children2[j]);

                if (same) {
                    foundSame = true;
                    matched[j] = true;
                    break;
                }
            }

            if (!foundSame)
                return false;
        }

        return true;
    }

    //2021.2.4 modified by meng xu.
    public static void main(String[] args) {
//        GPNode node1 = new Mul();
//        node1.children = new GPNode[2];
//        node1.children[0] = new AttributeGPNode(JobShopAttribute.DUE_DATE);
//        node1.children[1] = new AttributeGPNode(JobShopAttribute.PROC_TIME);
//
//        GPNode node2 = new Div();
//        node2.children = new GPNode[2];
//        node2.children[0] = new AttributeGPNode(JobShopAttribute.PROC_TIME);
//        node2.children[1] = new AttributeGPNode(JobShopAttribute.DUE_DATE);
//
//        System.out.println(equals(node1, node2));

        GPTree tree1 = new GPTree();
        GPNode node1 = new Mul();
        node1.children = new GPNode[2];
        node1.children[0] = new Div();
        node1.children[0].children = new GPNode[2];
        node1.children[0].children[0] = new AttributeGPNode(JobShopAttribute.PROC_TIME);
        node1.children[0].children[1] = new AttributeGPNode(JobShopAttribute.PROC_TIME);
        node1.children[1] = new AttributeGPNode(JobShopAttribute.PROC_TIME);
        tree1.child = node1;

//        GPTree tree2 = new GPTree();
//        GPNode node2 = new Div();
//        node2.children = new GPNode[2];
//        node2.children[0] = new AttributeGPNode(JobShopAttribute.PROC_TIME);
//        node2.children[1] = new AttributeGPNode(JobShopAttribute.DUE_DATE);
//        tree2.child = node2;

        GPTree tree2 = new GPTree();
        GPNode node2 = new Mul();
        node2.children = new GPNode[2];
        node2.children[0] = new Mul();
        node2.children[0].children = new GPNode[2];
        node2.children[0].children[0] = new AttributeGPNode(JobShopAttribute.PROC_TIME);
        node2.children[0].children[1] = new AttributeGPNode(JobShopAttribute.PROC_TIME);
        node2.children[1] = new AttributeGPNode(JobShopAttribute.PROC_TIME);
        tree2.child = node2;

        System.out.println(TreeEquals(tree1, tree2));

    }
}
