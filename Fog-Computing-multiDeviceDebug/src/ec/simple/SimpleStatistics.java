/*
  Copyright 2006 by Sean Luke
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/


package ec.simple;
import ec.*;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.gp.GPTree;
import ec.steadystate.*;
import java.io.IOException;
import ec.util.*;
import mengxu.gp.terminal.AttributeGPNode;
import mengxu.gp.terminal.AttributeProvider;
import mengxu.gp.terminal.JobShopAttribute;
//import mengxu.algorithm.ensemble.GPRuleEvolutionStateCluster;
import qgl.CustomFitness;
import java.io.File;
import java.util.*;
import java.util.Arrays;
/* 
 * SimpleStatistics.java
 * 
 * Created: Tue Aug 10 21:10:48 1999
 * By: Sean Luke
 */

/**
 * A basic Statistics class suitable for simple problem applications.
 *
 * SimpleStatistics prints out the best individual, per subpopulation,
 * each generation.  At the end of a run, it also prints out the best
 * individual of the run.  SimpleStatistics outputs this data to a log
 * which may either be a provided file or stdout.  Compressed files will
 * be overridden on restart from checkpoint; uncompressed files will be 
 * appended on restart.
 *
 * <p>SimpleStatistics implements a simple version of steady-state statistics:
 * if it quits before a generation boundary,
 * it will include the best individual discovered, even if the individual was discovered
 * after the last boundary.  This is done by using individualsEvaluatedStatistics(...)
 * to update best-individual-of-generation in addition to doing it in
 * postEvaluationStatistics(...).

 <p><b>Parameters</b><br>
 <table>
 <tr><td valign=top><i>base.</i><tt>gzip</tt><br>
 <font size=-1>boolean</font></td>
 <td valign=top>(whether or not to compress the file (.gz suffix added)</td></tr>
 <tr><td valign=top><i>base.</i><tt>file</tt><br>
 <font size=-1>String (a filename), or nonexistant (signifies stdout)</font></td>
 <td valign=top>(the log for statistics)</td></tr>
 </table>

 *
 * @author Sean Luke
 * @version 1.0 
 */

public class SimpleStatistics extends Statistics implements SteadyStateStatisticsForm //, ec.eval.ProvidesBestSoFar
    {
    public Individual[] getBestSoFar() { return best_of_run; }

    /** log file parameter */
    public static final String P_STATISTICS_FILE = "file";
    
    /** compress? */
    public static final String P_COMPRESS = "gzip";
    
    public static final String P_DO_FINAL = "do-final";
    public static final String P_DO_GENERATION = "do-generation";
    public static final String P_DO_MESSAGE = "do-message";
    public static final String P_DO_DESCRIPTION = "do-description";
    public static final String P_DO_PER_GENERATION_DESCRIPTION = "do-per-generation-description";

    /** The Statistics' log */
    public int statisticslog = 0;  // stdout

    /** The best individual we've found so far */
    public Individual[] best_of_run = null;
        private List<Integer> bestSeqLengths = new ArrayList<>();
        private List<Integer> bestSeqFeatures = new ArrayList<>();
        private List<Integer> bestRoutLengths = new ArrayList<>();
        private List<Integer> bestRoutFeatures = new ArrayList<>();
    /** Should we compress the file? */
    public boolean compress;
    public boolean doFinal;
    public boolean doGeneration;
    public boolean doMessage;
    public boolean doDescription;
    public boolean doPerGenerationDescription;

    public void setup(final EvolutionState state, final Parameter base)
        {
        super.setup(state,base);
        
        compress = state.parameters.getBoolean(base.push(P_COMPRESS),null,false);
                
        File statisticsFile = state.parameters.getFile(
            base.push(P_STATISTICS_FILE),null);

        doFinal = state.parameters.getBoolean(base.push(P_DO_FINAL),null,true);
        doGeneration = state.parameters.getBoolean(base.push(P_DO_GENERATION),null,true);
        doMessage = state.parameters.getBoolean(base.push(P_DO_MESSAGE),null,true);
        doDescription = state.parameters.getBoolean(base.push(P_DO_DESCRIPTION),null,true);
        doPerGenerationDescription = state.parameters.getBoolean(base.push(P_DO_PER_GENERATION_DESCRIPTION),null,false);

        if (silentFile)
            {
            statisticslog = Output.NO_LOGS;
            }
        else if (statisticsFile!=null)
            {
            try
                {
                statisticslog = state.output.addLog(statisticsFile, !compress, compress);
                }
            catch (IOException i)
                {
                state.output.fatal("An IOException occurred while trying to create the log " + statisticsFile + ":\n" + i);
                }
            }
        else state.output.warning("No statistics file specified, printing to stdout at end.", base.push(P_STATISTICS_FILE));
        }

    public void postInitializationStatistics(final EvolutionState state)
        {
        super.postInitializationStatistics(state);
        
        // set up our best_of_run array -- can't do this in setup, because
        // we don't know if the number of subpopulations has been determined yet
        best_of_run = new Individual[state.population.subpops.length];
        }

    /** Logs the best individual of the generation. */
    boolean warned = false;
    public void postEvaluationStatistics(final EvolutionState state)
        {
        super.postEvaluationStatistics(state);
            // 遍历每个子种群

            for(int x=0;x<state.population.subpops.length;x++) {

                Subpopulation subpop = state.population.subpops[x];

                Individual[] individuals = subpop.individuals;



                Individual bestIndividual = null;

                double[] bestMakespan = null;



                // 遍历个体，找到本代最佳 makespan

                for (Individual ind : individuals) {

                    if (ind == null) continue;



                    // 获取 makespan

                    CustomFitness fitness = (CustomFitness) ind.fitness;

                    double[] currentMakespan = fitness.getMakespan();



                    // 比较逻辑：假设 makespan 越小越好

                    if (bestIndividual == null || currentMakespan[0] < bestMakespan[0]) { // 单目标示例

                        bestIndividual = ind;

                        bestMakespan = currentMakespan;

                    }

                }



                // 更新 best_of_run

                if (best_of_run[x] == null ||

                        bestMakespan[0] < ((CustomFitness)best_of_run[x].fitness).getMakespan()[0]) {

                    best_of_run[x] = (Individual) bestIndividual.clone();

                }



                // 输出逻辑保持不变，仅在打印时添加 makespan 信息

                if (doGeneration) {

                    state.output.println("Subpopulation " + x + " Best Makespan: " + Arrays.toString(bestMakespan), statisticslog);

                }

            }
        // for now we just print the best fitness per subpopulation.
        Individual[] best_i = new Individual[state.population.subpops.length];  // quiets compiler complaints
            List<Integer> sequencingRuleLengths = new ArrayList<>();
            List<Integer> sequencingFeatureCounts = new ArrayList<>();
            List<Integer> routingRuleLengths = new ArrayList<>();
            List<Integer> routingFeatureCounts = new ArrayList<>();
            // **存储统计数据**
            double totalRuleLength = 0;
            double totalFeatureCount = 0;
            int numIndividuals = 0;  // 统计个体总数，避免除零错误



            for(int x=0;x<state.population.subpops.length;x++)
            {
            best_i[x] = state.population.subpops[x].individuals[0];
            for(int y=1;y<state.population.subpops[x].individuals.length;y++)
                {
                if (state.population.subpops[x].individuals[y] == null)
                    {
                    if (!warned)
                        {
                        state.output.warnOnce("Null individuals found in subpopulation");
                        warned = true;  // we do this rather than relying on warnOnce because it is much faster in a tight loop
                        }
                    }
                else if (best_i[x] == null || state.population.subpops[x].individuals[y].fitness.betterThan(best_i[x].fitness))
                    best_i[x] = state.population.subpops[x].individuals[y];

                if (best_i[x] == null)
                    {
                    if (!warned)
                        {
                        state.output.warnOnce("Null individuals found in subpopulation");
                        warned = true;  // we do this rather than relying on warnOnce because it is much faster in a tight loop
                        }
                    }
                }
                GPIndividual bestIndividual = (GPIndividual) best_i[0];
                int bestSeqLen = countNodes(bestIndividual.trees[0].child);
                int bestSeqFeat = countUniqueAttributes(bestIndividual.trees[0].child);
                int bestRoutLen = countNodes(bestIndividual.trees[1].child);
                int bestRoutFeat = countUniqueAttributes(bestIndividual.trees[1].child);

                bestSeqLengths.add(bestSeqLen);
                bestSeqFeatures.add(bestSeqFeat);
                bestRoutLengths.add(bestRoutLen);
                bestRoutFeatures.add(bestRoutFeat);
                if (bestSeqLengths.size() > 50) {
                    bestSeqLengths.remove(0);
                    bestSeqFeatures.remove(0);
                    bestRoutLengths.remove(0);
                    bestRoutFeatures.remove(0);
                }

                double avgBestSeqLen = calculateMean(bestSeqLengths);
                double avgBestSeqFeat = calculateMean(bestSeqFeatures);
                double avgBestRoutLen = calculateMean(bestRoutLengths);
                double avgBestRoutFeat = calculateMean(bestRoutFeatures);

                state.output.message("Generation " + state.generation +
                        " | Best SEQ Len: " + bestSeqLen + " | Best SEQ Feat: " + bestSeqFeat +
                        " | Best ROUT Len: " + bestRoutLen + " | Best ROUT Feat: " + bestRoutFeat);

                state.output.message("Last 50 Gen Avg | SEQ Len: " + avgBestSeqLen + " | SEQ Feat: " + avgBestSeqFeat +
                        " | ROUT Len: " + avgBestRoutLen + " | ROUT Feat: " + avgBestRoutFeat);

                // **计算规则长度 & 特征数**
                for (Individual ind : state.population.subpops[x].individuals) {
                    if (ind instanceof GPIndividual) {
                        GPIndividual gpInd = (GPIndividual) ind;
                        // 遍历个体的两棵树
                        GPTree sequencingTree = gpInd.trees[0];  // SEQUENCING
                        GPTree routingTree = gpInd.trees[1];     // ROUTING
                        // **SEQUENCING 规则**
                        int seqRuleLength = countNodes(sequencingTree.child);
                        int seqFeatureCount = countUniqueAttributes(sequencingTree.child);
                        sequencingRuleLengths.add(seqRuleLength);
                        sequencingFeatureCounts.add(seqFeatureCount);

                        // **ROUTING 规则**
                        int routRuleLength = countNodes(routingTree.child);
                        int routFeatureCount = countUniqueAttributes(routingTree.child);
                        routingRuleLengths.add(routRuleLength);
                        routingFeatureCounts.add(routFeatureCount);
                        //System.out.println( routRuleLength);
                        for (GPTree tree : gpInd.trees) {
                            int ruleLength = countNodes(tree.child);
                            int featureCount = countUniqueAttributes(tree.child);

                            totalRuleLength += ruleLength;
                            totalFeatureCount += featureCount;
                            numIndividuals++;
                        }
                    }
                }

                // now test to see if it's the new best_of_run
            if (best_of_run[x]==null || best_i[x].fitness.betterThan(best_of_run[x].fitness))
                best_of_run[x] = (Individual)(best_i[x].clone());
            }
            // **计算统计值**
            double avgSeqRuleLength = calculateMean(sequencingRuleLengths);
            double varSeqRuleLength = calculateVariance(sequencingRuleLengths, avgSeqRuleLength);
            int minSeqRuleLength = Collections.min(sequencingRuleLengths);

            double avgSeqFeatureCount = calculateMean(sequencingFeatureCounts);
            double varSeqFeatureCount = calculateVariance(sequencingFeatureCounts, avgSeqFeatureCount);
            int minSeqFeatureCount = Collections.min(sequencingFeatureCounts);

            double avgRoutRuleLength = calculateMean(routingRuleLengths);
            double varRoutRuleLength = calculateVariance(routingRuleLengths, avgRoutRuleLength);
            int minRoutRuleLength = Collections.min(routingRuleLengths);

            double avgRoutFeatureCount = calculateMean(routingFeatureCounts);
            double varRoutFeatureCount = calculateVariance(routingFeatureCounts, avgRoutFeatureCount);
            int minRoutFeatureCount = Collections.min(routingFeatureCounts);


            // **输出日志**
            state.output.println("\nGeneration: " + state.generation, statisticslog);
            state.output.println("SEQUENCING - Avg Rule Length: " + avgSeqRuleLength + " | Variance: " + varSeqRuleLength + " | Min: " + minSeqRuleLength, statisticslog);
            state.output.println("SEQUENCING - Avg Feature Count: " + avgSeqFeatureCount + " | Variance: " + varSeqFeatureCount + " | Min: " + minSeqFeatureCount, statisticslog);
            state.output.println("ROUTING - Avg Rule Length: " + avgRoutRuleLength + " | Variance: " + varRoutRuleLength + " | Min: " + minRoutRuleLength, statisticslog);
            state.output.println("ROUTING - Avg Feature Count: " + avgRoutFeatureCount + " | Variance: " + varRoutFeatureCount + " | Min: " + minRoutFeatureCount, statisticslog);
            if (doMessage && !silentPrint) {
                state.output.message("Generation " + state.generation + " | SEQ - Avg Len: " + avgSeqRuleLength + " | SEQ - Avg Features: " + avgSeqFeatureCount+ " | Min Len: " + minSeqRuleLength );
                state.output.message("Generation " + state.generation + " | ROUT - Avg Len: " + avgRoutRuleLength + " | ROUT - Avg Features: " + avgRoutFeatureCount+" | Min Len: " + minRoutRuleLength );
            }
            // **计算平均值**
            double avgRuleLength = numIndividuals > 0 ? totalRuleLength / numIndividuals : 0;
            double avgFeatureCount = numIndividuals > 0 ? totalFeatureCount / numIndividuals : 0;
            // **输出统计信息**
            state.output.println("\nGeneration: " + state.generation, statisticslog);
            state.output.println("Average Rule Length: " + avgRuleLength, statisticslog);
            state.output.println("Average Feature Count: " + avgFeatureCount, statisticslog);

            if (doMessage && !silentPrint) {
                state.output.message("Generation " + state.generation + " | Avg Rule Length: " + avgRuleLength + " | Avg Feature Count: " + avgFeatureCount);
            }
            // print the best-of-generation individual
        if (doGeneration) state.output.println("\nGeneration: " + state.generation,statisticslog);
        if (doGeneration) state.output.println("Best Individual:",statisticslog);
        for(int x=0;x<state.population.subpops.length;x++)
            {
            if (doGeneration) state.output.println("Subpopulation " + x + ":",statisticslog);
            if (doGeneration) best_i[x].printIndividualForHumans(state,statisticslog);

                // 添加 makespan 输出
            CustomFitness fitness = (CustomFitness) best_i[x].fitness;

                if (doMessage && !silentPrint) {

                    state.output.message("Subpop " + x + " best makespan: " + Arrays.toString(fitness.getMakespan()));

                }

            if (doMessage && !silentPrint) state.output.message("Subpop " + x + " best fitness of generation" +
                (best_i[x].evaluated ? " " : " (evaluated flag not set): ") +
                best_i[x].fitness.fitnessToStringForHumans());
                
            // describe the winner if there is a description
            if (doGeneration && doPerGenerationDescription) 
                {
                if (state.evaluator.p_problem instanceof SimpleProblemForm)
                    ((SimpleProblemForm)(state.evaluator.p_problem.clone())).describe(state, best_i[x], x, 0, statisticslog);   
                }   
            }

        //modified by mengxu 2021.05.08
//        if(state instanceof GPRuleEvolutionStateCluster){
////            if (doGeneration) state.output.println("\nGeneration: " + state.generation,statisticslog);
////            if (doGeneration) state.output.println("Best Individual:",statisticslog);
//            for(int x=0;x<state.population.subpops.length;x++)
//            {
//                if (doGeneration) state.output.println("Subpopulation " + x + ":",statisticslog);
//                if (doGeneration) best_i[x].printIndividualForHumans(state,statisticslog);
//                //print the individuals in ensemble
//                for(int i=0; i<((GPRuleEvolutionStateCluster) state).getEnsemble().size(); i++){
//
//                }
//                if (doMessage && !silentPrint){
//                    int ensembleSize = ((GPRuleEvolutionStateCluster) state).getEnsemble().size();
//                    state.output.message("Ensemble size " + ensembleSize + " ensemble fitness: ");
//                    for(int i=0; i<ensembleSize; i++){
//                        state.output.message(((GPRuleEvolutionStateCluster) state).getEnsemble().get(i).fitness.fitnessToStringForHumans());
//                    }
//                }
//
//                // describe the winner if there is a description
//                if (doGeneration && doPerGenerationDescription)
//                {
//                    if (state.evaluator.p_problem instanceof SimpleProblemForm)
//                        ((SimpleProblemForm)(state.evaluator.p_problem.clone())).describe(state, best_i[x], x, 0, statisticslog);
//                }
//            }

//        }
        }

    /** Allows MultiObjectiveStatistics etc. to call super.super.finalStatistics(...) without
        calling super.finalStatistics(...) */
    protected void bypassFinalStatistics(EvolutionState state, int result)
        { super.finalStatistics(state, result); }
        private int countNodes(GPNode node) {
            if (node == null) return 0;
            int count = 1;
            for (GPNode child : node.children) {
                count += countNodes(child);
            }
            return count;
        }
        private double calculateMean(List<Integer> values) {
            if (values.isEmpty()) return 0.0;
            double sum = 0;
            for (int v : values) sum += v;
            return sum / values.size();
        }

        private double calculateVariance(List<Integer> values, double mean) {
            if (values.isEmpty()) return 0.0;
            double sum = 0;
            for (int v : values) sum += Math.pow(v - mean, 2);
            return sum / values.size();
        }

        /**
     * **统计唯一的 JobShopAttribute 数量（特征数）**
     */
        private int countUniqueAttributes(GPNode node) {
            Set<JobShopAttribute> attributes = new HashSet<>();
            traverseTreeForAttributes(node, attributes);
            return attributes.size();
        }
        /**
         * **遍历 GP 树，提取 JobShopAttribute**
         */
        private void traverseTreeForAttributes(GPNode node, Set<JobShopAttribute> attributes) {
            if (node == null) return;

            // ✅ 确保 TerminalERCUniform 也能正确解析特征
            if (node instanceof AttributeProvider) {
                //System.out.println(node.toString());
                JobShopAttribute attr = ((AttributeProvider) node).getJobShopAttribute();
                if (attr != null && !attributes.contains(attr)) {
                    //System.out.println("✅ 解析出的 JobShopAttribute: " + attr.toString());
                    attributes.add(attr);
                }
            }

            // 递归遍历子节点
            for (GPNode child : node.children) {
                traverseTreeForAttributes(child, attributes);
            }
        }
    /** Logs the best individual of the run. */
    public void finalStatistics(final EvolutionState state, final int result)
        {
        super.finalStatistics(state,result);
        
        // for now we just print the best fitness 
        
        if (doFinal) state.output.println("\nBest Individual of Run:",statisticslog);
        for(int x=0;x<state.population.subpops.length;x++ )
            {
            if (doFinal) state.output.println("Subpopulation " + x + ":",statisticslog);
            if (doFinal) best_of_run[x].printIndividualForHumans(state,statisticslog);
            if (doMessage && !silentPrint) state.output.message("Subpop " + x + " best fitness of run: " + best_of_run[x].fitness.fitnessToStringForHumans());
                // 添加 makespan 输出

            CustomFitness fitness = (CustomFitness) best_of_run[x].fitness;

                if (doMessage && !silentPrint) {

                    state.output.message("Subpop " + x + " best makespan of run: " + Arrays.toString(fitness.getMakespan()));}
            // finally describe the winner if there is a description
            if (doFinal && doDescription) 
                if (state.evaluator.p_problem instanceof SimpleProblemForm)
                    ((SimpleProblemForm)(state.evaluator.p_problem.clone())).describe(state, best_of_run[x], x, 0, statisticslog);      
            }
        }
    }
