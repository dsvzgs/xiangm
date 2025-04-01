/*
  Copyright 2006 by Sean Luke
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/


package ec.gp.koza;
import ec.*;
import ec.gp.*;
import ec.util.*;
import mengxu.gp.terminal.TerminalERCUniform;
import mengxu.rule.RuleType;
//import mengxu.algorithm.multipletreegp.GPRuleEvolutionStateMV1;
//import mengxu.mcts.MctsTreeNode;
//import yimei.jss.gp.terminal.AttributeGPNode;

/* 
 * KozaBuilder.java
 * 
 * Created: Sun Oct 29 22:35:34 EST 2006
 * By: Sean Luke
 */

/*
  KozaBuilder is an abstract superclass of three tree builders: GROW, FULL, and RAMPED HALF-AND-HALF,
  all described in I/II.  As all three classes specify a minimum and maximum depth, these instance
  variables and setup methods appear here; but they are described in detail in the relevant subclasses
  (GrowBuilder, HalfBuilder, and FullBuilder).

  <p><b>Parameters</b><br>
  <table>
  <tr><td valign=top><i>base</i>.<tt>min-depth</tt><br>
  <font size=-1>int &gt;= 1</font></td>
  <td valign=top>(smallest "maximum" depth the builder may use for building a tree.  2 is the default.)</td></tr>

  <tr><td valign=top><i>base</i>.<tt>max-depth</tt><br>
  <font size=-1>int &gt;= <i>base</i>.<tt>min-depth</tt></font></td>
  <td valign=top>(largest "maximum" depth the builder may use for building a tree. 6 is the default.)</td></tr>
  </table>

  @author Sean Luke
  @version 1.0 
*/

public abstract class KozaBuilder extends GPNodeBuilder
    {
    public static final String P_MAXDEPTH = "max-depth";
    public static final String P_MINDEPTH = "min-depth";

    public static final String P_MCTS_POLICY = "mcts-policy";

    public String mctsPolicy;

    /** The largest maximum tree depth RAMPED HALF-AND-HALF can specify. */
    public int maxDepth;

    /** The smallest maximum tree depth RAMPED HALF-AND-HALF can specify. */
    public int minDepth;

    public void setup(final EvolutionState state, final Parameter base)
        {
        super.setup(state,base);

        Parameter def = defaultBase();

        // load maxdepth and mindepth, check that maxdepth>0, mindepth>0, maxdepth>=mindepth
        maxDepth = state.parameters.getInt(base.push(P_MAXDEPTH),def.push(P_MAXDEPTH),1);
        if (maxDepth<=0)
            state.output.fatal("The Max Depth for a KozaBuilder must be at least 1.",
                base.push(P_MAXDEPTH),def.push(P_MAXDEPTH));
                        
        minDepth = state.parameters.getInt(base.push(P_MINDEPTH),def.push(P_MINDEPTH),1);
        if (minDepth<=0)
            state.output.fatal("The Min Depth for a KozaBuilder must be at least 1.",
                base.push(P_MINDEPTH),def.push(P_MINDEPTH));

        if (maxDepth<minDepth)
            state.output.fatal("Max Depth must be >= Min Depth for a KozaBuilder",
                base.push(P_MAXDEPTH),def.push(P_MAXDEPTH));

        this.mctsPolicy = state.parameters.getString(new Parameter(P_MCTS_POLICY), null);

        }
                
    /** A private recursive method which builds a FULL-style tree for newRootedTree(...) */
    protected GPNode fullNode(final EvolutionState state,
        final int current,
        final int max,
        final GPType type,
        final int thread,
        final GPNodeParent parent,
        final int argposition,
        final GPFunctionSet set) 
        {
        // fullNode can mess up if there are no available terminal for a given type.  If this occurs,
        // and we find ourselves unable to pick a terminal when we want to do so, we will issue a warning,
        // and pick a nonterminal, violating the "FULL" contract.  This can lead to pathological situations
        // where the system will continue to go on and on unable to stop because it can't pick a terminal,
        // resulting in running out of memory or some such.  But there are cases where we'd want to let
        // this work itself out.
        boolean triedTerminals = false;   // did we try -- and fail -- to fetch a terminal?
            GPNodeParent curr = parent;
            while (curr != null && !(curr instanceof GPTree)) {
                if (curr instanceof GPNode) {
                    curr = ((GPNode) curr).parent;  // 继续向上遍历
                } else {
                    break;
                }
            }
            RuleType ruleType = RuleType.ROUTING;  // 默认值
            if (curr instanceof GPTree) {
                GPTree tree = (GPTree) curr;
                GPIndividual individual = (GPIndividual) tree.owner;
                if (individual.trees.length > 0 && individual.trees[0] == tree) {
                    ruleType = RuleType.SEQUENCING;
                }
            }else {
                System.out.println("默认值");
            }
        //fzhang  20.6.2018  here, the t is always equal to 0.  So, the terminals always get the terminal[0], the first terminal set
        int t = type.type;
        GPNode[] terminals = set.terminals[t];
        GPNode[] nonterminals = set.nonterminals[t];
        GPNode[] nodes = set.nodes[t];          

        if (nodes.length == 0)
            errorAboutNoNodeWithType(type, state);   // total failure

        // pick a terminal when we're at max depth or if there are NO nonterminals
        if ((  current+1 >= max ||                                                      // Now pick if we're at max depth
                warnAboutNonterminal(nonterminals.length==0, type, false, state)) &&     // OR if there are NO nonterminals!
            // this will freak out the static checkers
            (triedTerminals = true) &&                                                  // [first set triedTerminals]
            terminals.length != 0)                                                      // AND if there are available terminal
            {
            GPNode n = (GPNode)(terminals[state.random[thread].nextInt(terminals.length)].lightClone());
            n.resetNode(state,thread,set,ruleType);  // give ERCs a chance to randomize
            n.argposition = (byte)argposition;
            n.parent = parent;
            return n;
            }
                        
        // else force a nonterminal unless we have no choice
        else
            {
            if (triedTerminals) warnAboutNoTerminalWithType(type, false, state);        // we tried terminal and we're here because there were none!
                                
            GPNode[] nodesToPick = set.nonterminals[type.type];
            if (nodesToPick==null || nodesToPick.length ==0)                            // no nonterminals, hope the guy knows what he's doing!
                nodesToPick = set.terminals[type.type];                                 // this can only happen with the warning about nonterminals above

            GPNode n = (GPNode)(nodesToPick[state.random[thread].nextInt(nodesToPick.length)].lightClone());
            n.resetNode(state,thread);  // give ERCs a chance to randomize
            n.argposition = (byte)argposition;
            n.parent = parent;

            // Populate the node...
            GPType[] childtypes = n.constraints(((GPInitializer)state.initializer)).childtypes;
            for(int x=0;x<childtypes.length;x++)
                n.children[x] = fullNode(state,current+1,max,childtypes[x],thread,n,x,set);

            return n;
            }
        }
        protected GPNode fullNode(final EvolutionState state,
                                  final int current, final int max, final GPType type,
                                  final int thread, final GPNodeParent parent,
                                  final int argposition, final GPFunctionSet set,
                                  RuleType ruleType)  // 🔥 新增 RuleType 参数
        {
            boolean triedTerminals = false;
            int t = type.type;
            GPNode[] terminals = set.terminals[t];
            GPNode[] nodes = set.nodes[t];

            if (nodes.length == 0) errorAboutNoNodeWithType(type, state);

            // ✅ 直接传递 RuleType，避免在 resetNode() 里错误获取
            if ((current + 1 >= max) && (triedTerminals = true) && terminals.length != 0) {
                GPNode n = (GPNode) (terminals[state.random[thread].nextInt(terminals.length)].lightClone());

                // ✅ 传递 RuleType 给 `resetNode()`
                if (n instanceof TerminalERCUniform) {
                    ((TerminalERCUniform) n).resetNode(state, thread, set, ruleType);
                }

                n.argposition = (byte) argposition;
                n.parent = parent;
                return n;
            } else {
                if (triedTerminals) warnAboutNoTerminalWithType(type, false, state);

                GPNode n = (GPNode) (nodes[state.random[thread].nextInt(nodes.length)].lightClone());
                if (n instanceof TerminalERCUniform) {
                    ((TerminalERCUniform) n).resetNode(state, thread, set, ruleType);
                }

                n.argposition = (byte) argposition;
                n.parent = parent;

                GPType[] childtypes = n.constraints(((GPInitializer) state.initializer)).childtypes;
                for (int x = 0; x < childtypes.length; x++)
                    n.children[x] = fullNode(state, current + 1, max, childtypes[x], thread, n, x, set, ruleType);

                return n;
            }
        }

        protected GPNode growNode(final EvolutionState state,
                                  final int current, final int max, final GPType type,
                                  final int thread, final GPNodeParent parent,
                                  final int argposition, final GPFunctionSet set,
                                  RuleType ruleType)  // 🔥 传递 RuleType
        {
            boolean triedTerminals = false;
            int t = type.type;
            GPNode[] terminals = set.terminals[t];
            GPNode[] nodes = set.nodes[t];

            if (nodes.length == 0) errorAboutNoNodeWithType(type, state);

            if ((current + 1 >= max) && (triedTerminals = true) && terminals.length != 0) {
                GPNode n = (GPNode) (terminals[state.random[thread].nextInt(terminals.length)].lightClone());

                // ✅ 传递 RuleType
                if (n instanceof TerminalERCUniform) {
                    ((TerminalERCUniform) n).resetNode(state, thread, set, ruleType);
                }

                n.argposition = (byte) argposition;
                n.parent = parent;
                return n;
            } else {
                if (triedTerminals) warnAboutNoTerminalWithType(type, false, state);

                GPNode n = (GPNode) (nodes[state.random[thread].nextInt(nodes.length)].lightClone());
                if (n instanceof TerminalERCUniform) {
                    ((TerminalERCUniform) n).resetNode(state, thread, set, ruleType);
                }

                n.argposition = (byte) argposition;
                n.parent = parent;

                GPType[] childtypes = n.constraints(((GPInitializer) state.initializer)).childtypes;
                for (int x = 0; x < childtypes.length; x++)
                    n.children[x] = growNode(state, current + 1, max, childtypes[x], thread, n, x, set, ruleType);

                return n;
            }
        }
        private GPNodeParent findGPTreeParent(GPNodeParent parent) {
            while (parent != null && !(parent instanceof GPTree)) {
                if (parent instanceof GPNode) {
                    parent = ((GPNode) parent).parent;
                } else {
                    break;
                }
            }
            return parent;
        }
    /** A private function which recursively returns a GROW tree to newRootedTree(...) */
    protected GPNode growNode(final EvolutionState state,
        final int current,
        final int max,
        final GPType type,
        final int thread,
        final GPNodeParent parent,
        final int argposition,
        final GPFunctionSet set)
        {
        // growNode can mess up if there are no available terminal for a given type.  If this occurs,
        // and we find ourselves unable to pick a terminal when we want to do so, we will issue a warning,
        // and pick a nonterminal, violating the maximum-depth contract.  This can lead to pathological situations
        // where the system will continue to go on and on unable to stop because it can't pick a terminal,
        // resulting in running out of memory or some such.  But there are cases where we'd want to let
        // this work itself out.
        boolean triedTerminals = false;
            GPNodeParent curr = parent;
            while (curr != null && !(curr instanceof GPTree)) {
                if (curr instanceof GPNode) {
                    curr = ((GPNode) curr).parent;  // 继续向上遍历
                } else {
                    break;
                }
            }
            RuleType ruleType = RuleType.ROUTING;  // 默认值
            if (curr instanceof GPTree) {
                GPTree tree = (GPTree) curr;
                GPIndividual individual = (GPIndividual) tree.owner;
                if (individual.trees.length > 0 && individual.trees[0] == tree) {
                    ruleType = RuleType.SEQUENCING;
                }
            }else {
                System.out.println("默认值");
            }
        int t = type.type;
        GPNode[] terminals = set.terminals[t];
        // GPNode[] nonterminals = set.nonterminals[t];
        GPNode[] nodes = set.nodes[t];


        if (nodes.length == 0)
            errorAboutNoNodeWithType(type, state);   // total failure

        // pick a terminal when we're at max depth or if there are NO nonterminals
        if ((current+1 >= max) &&                                                       // Now pick if we're at max depth
            // this will freak out the static checkers
            (triedTerminals = true) &&                                                  // [first set triedTerminals]
            terminals.length != 0)                                                      // AND if there are available terminal
            {
            GPNode n = (GPNode)(terminals[state.random[thread].nextInt(terminals.length)].lightClone());
           // RuleType ruleType = (parent.toString().contains("T0")) ? RuleType.SEQUENCING : RuleType.ROUTING;
            n.resetNode(state,thread,set,ruleType);  // give ERCs a chance to randomize
            n.argposition = (byte)argposition;
            n.parent = parent;


            return n;
            }
                        
        // else pick a random node
        else
            {
            if (triedTerminals) warnAboutNoTerminalWithType(type, false, state);        // we tried terminal and we're here because there were none!
              //  System.out.println(parent);
            GPNode n = (GPNode)(nodes[state.random[thread].nextInt(nodes.length)].lightClone());
         //       RuleType ruleType = (parent.toString().contains("T0")) ? RuleType.SEQUENCING : RuleType.ROUTING;
            n.resetNode(state,thread,set,ruleType);  // give ERCs a chance to randomize
            n.argposition = (byte)argposition;
            n.parent = parent;

            // Populate the node...
            GPType[] childtypes = n.constraints(((GPInitializer)state.initializer)).childtypes;

            for(int x=0;x<childtypes.length;x++)
                n.children[x] = growNode(state,current+1,max,childtypes[x],thread,n,x,set);

            return n;
            }
        }

//        /** modified by mengxu 2021.04.08 **/
//        /** A private function which recursively returns a GROW tree to newRootedTree(...) */
//        protected GPNode growNodeBasedOnMcts(final EvolutionState state,
//                                             final int current,
//                                             final int max,
//                                             final GPType type,
//                                             final int thread,
//                                             final GPNodeParent parent,
//                                             final int argposition,
//                                             final GPFunctionSet set,
//                                             int treeNum,
//                                             MctsTreeNode mctsTreeNode)
//        {
//            // growNode can mess up if there are no available terminal for a given type.  If this occurs,
//            // and we find ourselves unable to pick a terminal when we want to do so, we will issue a warning,
//            // and pick a nonterminal, violating the maximum-depth contract.  This can lead to pathological situations
//            // where the system will continue to go on and on unable to stop because it can't pick a terminal,
//            // resulting in running out of memory or some such.  But there are cases where we'd want to let
//            // this work itself out.
//            boolean triedTerminals = false;
//
//            int t = type.type;
//            GPNode[] terminals = set.terminals[t];
//            // GPNode[] nonterminals = set.nonterminals[t];
//            GPNode[] nodes = set.nodes[t];
//
//            if (nodes.length == 0)
//                errorAboutNoNodeWithType(type, state);   // total failure
//
//
//
//            // pick a terminal when we're at max depth or if there are NO nonterminals
//            if ((current+1 >= max) &&                                                       // Now pick if we're at max depth
//                    // this will freak out the static checkers
//                    (triedTerminals = true) &&                                                  // [first set triedTerminals]
//                    terminals.length != 0)                                                      // AND if there are available terminal
//            {
////                GPNode n = mctsTreeNode.expandNewTerminalRandom(state, thread);
//                GPNode n;
//                if(this.mctsPolicy.equals("mean-non-infinity-reward")){
//                    n = mctsTreeNode.expandNewTerminalBasedOnMinMeanNonInfinityReward(state, thread);
//                }
//                else if(this.mctsPolicy.equals("num-infinity-reward")){
//                    n = mctsTreeNode.expandNewTerminalBasedOnNumInfinityReward(state, thread);
//                }
//                else if(this.mctsPolicy.equals("totalReward-div-visitCount-linked")){
//                    n = mctsTreeNode.expandNewTerminalBasedOnTRandVCV1(state, thread);
//                }
//                else if(this.mctsPolicy.equals("totalReward-div-visitCount")){
//                    n = mctsTreeNode.expandNewTerminalBasedOnTRandVC(state, thread);
//                }
//                else if(this.mctsPolicy.equals("visitCount")){
//                    n = mctsTreeNode.expandNewTerminalBasedOnVisitCount(state, thread);
//                }
//                else if(this.mctsPolicy.equals("random")){
//                    n = mctsTreeNode.expandNewTerminalRandom(state, thread);
//                }else if(this.mctsPolicy.equals("hybird")){
//                    n = mctsTreeNode.expandNewTerminalHybird(state, thread);
//                }else if(this.mctsPolicy.equals("hybirdv1")){
//                    n = mctsTreeNode.expandNewTerminalHybirdV1(state, thread);
//                }
//                else if(this.mctsPolicy.equals("mean-non-infinity-reward-pro")){
//                    n = mctsTreeNode.expandNewTerminalBasedOnMinMeanNonInfinityRewardWithProbability(state, thread);
//                }else if(this.mctsPolicy.equals("num-infinity-reward-pro")){
//                    n = mctsTreeNode.expandNewTerminalBasedOnNumInfinityRewardWithProbability(state, thread);
//                }else if(this.mctsPolicy.equals("mean-non-infinity-reward-linked-pro")){
//                    n = mctsTreeNode.expandNewTerminalBasedOnMinMeanNonInfinityRewardLinkedWithProbability(state, thread);
//                }else{
//                    n = null;
//                    System.out.println("Wrong! need mctsPolicy parameter.");
//                }
//
////                mctsTreeNode = mctsTreeNode.getCurrentChild();
////                GPNode n = state.mctses[treeNum].getMctsRootNode().expandNewTerminalRandom();
////                GPNode n = (GPNode)(terminals[state.random[thread].nextInt(terminals.length)].lightClone());
//                n.resetNode(state,thread,set);  // give ERCs a chance to randomize
//                n.argposition = (byte)argposition;
//                n.parent = parent;
//                return n;
//            }
//
//            // else pick a random node
//            else
//            {
//                if (triedTerminals) warnAboutNoTerminalWithType(type, false, state);        // we tried terminal and we're here because there were none!
//
////                GPNode n = mctsTreeNode.expandNewChildRandom(state, thread);
//                GPNode n;
//                if(this.mctsPolicy.equals("mean-non-infinity-reward")){
//                    n = mctsTreeNode.expandNewChildBasedOnMinMeanNonInfinityReward(state, thread);
//                }
//                else if(this.mctsPolicy.equals("num-infinity-reward")){
//                    n = mctsTreeNode.expandNewChildBasedOnNumInfinityReward(state, thread);
//                }
//                else if(this.mctsPolicy.equals("totalReward-div-visitCount-linked")){
//                    n = mctsTreeNode.expandNewChildBasedOnTRandVCV1(state, thread);
//                }
//                else if(this.mctsPolicy.equals("totalReward-div-visitCount")){
//                    n = mctsTreeNode.expandNewChildBasedOnTRandVC(state, thread);
//                }
//                else if(this.mctsPolicy.equals("visitCount")){
//                    n = mctsTreeNode.expandNewChildBasedOnVisitCount(state, thread);
//                }
//                else if(this.mctsPolicy.equals("random")){
//                    n = mctsTreeNode.expandNewChildRandom(state, thread);
//                }else if(this.mctsPolicy.equals("hybird")){
//                    n = mctsTreeNode.expandNewChildHybird(state, thread);
//                }else if(this.mctsPolicy.equals("hybirdv1")){
//                    n = mctsTreeNode.expandNewChildHybirdV1(state, thread);
//                }else if(this.mctsPolicy.equals("mean-non-infinity-reward-pro")){
//                    n = mctsTreeNode.expandNewChildBasedOnMinMeanNonInfinityRewardWithProbability(state, thread);
//                }else if(this.mctsPolicy.equals("num-infinity-reward-pro")){
//                    n = mctsTreeNode.expandNewChildBasedOnNumInfinityRewardWithProbability(state, thread);
//                }else if(this.mctsPolicy.equals("mean-non-infinity-reward-linked-pro")){
//                    n = mctsTreeNode.expandNewChildBasedOnMinMeanNonInfinityRewardLinkedWithProbability(state, thread);
//                }else{
//                    n = null;
//                    System.out.println("Wrong! need mctsPolicy parameter.");
//                }
////                GPNode n = mctsTreeNode.expandNewChildBasedOnVisitCount(state, thread);
//                mctsTreeNode = mctsTreeNode.getCurrentChild();
////                GPNode n = (GPNode)(nodes[state.random[thread].nextInt(nodes.length)].lightClone());
//                n.resetNode(state,thread,set);  // give ERCs a chance to randomize
//                n.argposition = (byte)argposition;
//                n.parent = parent;
//
//                // Populate the node...
//                GPType[] childtypes = n.constraints(((GPInitializer)state.initializer)).childtypes;
//                if(!(n instanceof AttributeGPNode) && !(n instanceof ERC)){//modified
//                    //todo: add ERC into the list of MCTS terminal
//                    //todo: check again if is the ERC cause the problem!!!
//                    n.children = new GPNode[2];
//                    for(int x=0;x<n.children.length;x++)
//                        n.children[x] = growNodeBasedOnMcts(state,current+1,max,childtypes[x],thread,n,x,set,treeNum, mctsTreeNode);
//                }
//
//                return n;
//            }
//        }


    }
