package mengxu.ruleevaluation;

import ec.EvolutionState;
import ec.Fitness;
import ec.multiobjective.MultiObjectiveFitness;
import ec.gp.GPNode;
import mengxu.algorithm.FCFS;
import mengxu.rule.AbstractRule;
import mengxu.rule.RuleType;
import mengxu.simulation.DynamicSimulation;
import mengxu.taskscheduling.Server;
//import yimei.jss.jobshop.WorkCenter;
//import yimei.jss.rule.AbstractRule;
//import yimei.jss.simulation.Simulation;
import mengxu.rule.evolved.GPRule;
import mengxu.gp.GPNodeComparator;
import java.util.List;
import qgl.CustomFitness;

public class MultipleTreeMultipleRuleEvaluationModel extends MultipleRuleEvaluationModel{
    int minComplexity = 10;
    int maxComplexity = 120;
    //========================rule evaluatation============================
    @Override
    public void evaluate(List<Fitness> currentFitnesses,
                         List<AbstractRule> rules,
                         EvolutionState state) {
        //expecting 2 rules here - one routing rule and one sequencing rule
        if (rules.size() != 2) {
            System.out.println("Rule evaluation failed!");
            System.out.println("Expecting 2 rules, only 1 found.");
            return;
        }

        //System.out.println(rules.size()); //2 repeat
        countInd++;

        AbstractRule sequencingRule = rules.get(0); // for each arraylist in list, they have two elements, the first one is sequencing rule and the second one is routing rule
        AbstractRule routingRule = rules.get(1);
        //System.out.println(sequencingRule);  //"GPRule"  repeat
        //System.out.println(routingRule);   //"GPRule"  repeat

        //System.out.println(objectives.size()); //1  repeat
        //code taken from Abstract Rule
        double[] fitnesses = new double[objectives.size()];
        double[] adjustedFitnesses = new double[objectives.size()]; // 带 ISP 惩罚的适应度（用于进化）
        List<DynamicSimulation> simulations = schedulingSet.getSimulations();
        int col = 0;


        //qgl
        // ✅ Step 2: 计算 minComplexity 和 maxComplexity
        // int minComplexity = Integer.MAX_VALUE;
        //int maxComplexity = Integer.MIN_VALUE;
        int complexity = 1;
        for (AbstractRule rule : rules) {
            if (rule instanceof GPRule) {
                // 确保是 GP 规则
                GPNode rootNode = ((GPRule) rule).getGPTree().child;
                complexity += GPNodeComparator.countNodes(rootNode);
                // int complexity = GPNodeComparator.countNodes(seqRootNode) + GPNodeComparator.countNodes(routeRootNode);
            }
        }
        if (complexity < minComplexity) minComplexity = complexity;
        if (complexity > maxComplexity) maxComplexity = complexity;
        //System.out.println("minComplexity: " + minComplexity + " maxComplexity: " + maxComplexity);
        //System.out.println(simulations.size()); // 1 repeat
        //System.out.println(schedulingSet.getReplications().get(0)); //1 repeat

        for (int j = 0; j < simulations.size(); j++) {
            DynamicSimulation simulation = simulations.get(j);
            //sequencingRule = null;
            //========================change here======================================
            //this is used for single tree (routing rule) evaluation
            if(sequencingRule == null){
                simulation.setSequencingRule(new FCFS(RuleType.SEQUENCING)); //indicate different individuals
                simulation.setRoutingRule(routingRule);
            }
            else{
                simulation.setSequencingRule(sequencingRule); //indicate different individuals
                simulation.setRoutingRule(routingRule);
            }

            //System.out.println(simulation);
            simulation.run();

            for (int i = 0; i < objectives.size(); i++) {
                //2018.10.23  cancel normalized process
//                double normObjValue = simulation.objectiveValue(objectives.get(i))  // this line: the value of makespan
//                        / schedulingSet.getObjectiveLowerBound(i, col);

                double ObjValue = simulation.objectiveValue(objectives.get(i)); // this line: the value of makespan


                //in essence, here is useless. because if w.numOpsInQueue() > 100, the simulation has been canceled in run(). here is a double check
                for (Server w: simulation.getSystemState().getServers()) {
                    if (w.numTaskInQueue() > 100) {
                        //this was a bad run
                        // ObjValue *= 1.5;
                        //fzhang cancel normalized process
//                      normObjValue = Double.MAX_VALUE;
                        ObjValue = Double.MAX_VALUE;

                        //System.out.println(systemState.getJobsInSystem().size());
                        //System.out.println(systemState.getJobsCompleted().size());

                        //normObjValue = normObjValue*(systemState.getJobsInSystem().size()/systemState.getJobsCompleted().size());
                        countBadrun++;
                        break;
                    }
                }

                //2018.10.23  cancel normalized process
//                fitnesses[i] += normObjValue;  //the value of fitness is the normalization of the objective value
                fitnesses[i] += ObjValue;
            }
            col++;

            //schedulingSet.getReplications().get(j) = 1, only calculate once, skip this part here
            for (int k = 1; k < schedulingSet.getReplications().get(j); k++) {
                simulation.rerun();

                for (int i = 0; i < objectives.size(); i++) {
                    //2018.10.23  cancel normalized process
//                    double normObjValue = simulation.objectiveValue(objectives.get(i))
//                            / schedulingSet.getObjectiveLowerBound(i, col);
//                    fitnesses[i] += normObjValue;

                    double ObjValue = simulation.objectiveValue(objectives.get(i));
                    fitnesses[i] += ObjValue;
                }

                col++;
            }

            simulation.reset();
        }

        //modified by fzhang 18.04.2018  in order to check this loop works or not after add filter part: does not work
        // if(countBadrun>0) {
        //System.out.println(state.generation);
        //System.out.println("The number of badrun grasped in model: "+ countBadrun);
        // }

        for (int i = 0; i < fitnesses.length; i++) {

            fitnesses[i] /= col;
            adjustedFitnesses[i] = fitnesses[i];
        }


        // ✅ Step 3: 计算 ISP 惩罚并调整 fitnesses
        //double ωPF = 0.2; // 惩罚最大比重 20%
        double ω4 = 0.3, ω5 = 0.4, ω6 = 0.3; // 3 个惩罚的权重

        double generationProgress = (double) state.generation / state.numGenerations;
        double ωPF = 0.2; //
        for (int i = 0; i < rules.size(); i++) {
            if (rules.get(i) instanceof GPRule) {
                GPNode rootNode = ((GPRule) rules.get(i)).getGPTree().child;

                // 计算 PF1（标准化复杂度）
                int numcplx = GPNodeComparator.countNodes(rootNode);
                double PF1 = (maxComplexity == minComplexity) ? 0.0 : (double) (numcplx ) / (maxComplexity - minComplexity);

                //   // 计算 PF2（除法运算符比例）
                int numDiv = GPNodeComparator.countDivOperators(rootNode);
                int numOp = GPNodeComparator.countTotalOperators(rootNode);
                double PF2 = (numOp == 0) ? 0.0 : (double) numDiv / (numOp + 1e-6);


                // 计算 PF3（动态元素比例）
                int[] terminalCounts = GPNodeComparator.countTerminalTypes(rootNode);
                int numStatic = terminalCounts[0];
                int numDynamic = terminalCounts[1];
                double PF3 = (numStatic + numDynamic == 0) ? 0.0 : (double) numDynamic / (numStatic + numDynamic);

                // 计算 ISP 惩罚值
                for (int j = 0; j < adjustedFitnesses.length; j++) {
                    double penalty = adjustedFitnesses[j] * ωPF * (ω4 * PF1 + ω5 * PF2 + ω6 * PF3);
                    adjustedFitnesses[j] += penalty;  // 进化过程中使用 ISP 罚值
                }

            }
        }
        //System.out.println(currentFitnesses.size()); //1
//        for (Fitness fitness: currentFitnesses) {
//            MultiObjectiveFitness f = (MultiObjectiveFitness) fitness;
//            f.setObjectives(state, fitnesses);
//        }

        // ✅ **Step 5: 在进化过程中使用 ISP 罚值**
        for (int i = 0; i < currentFitnesses.size(); i++) {
            CustomFitness f = (CustomFitness) currentFitnesses.get(i);  // 类型转换
            //MultiObjectiveFitness f = (MultiObjectiveFitness) currentFitnesses.get(i);
            f.setObjectives(state, adjustedFitnesses);  // 进化使用 ISP 罚值
            f.setMakespan(fitnesses.clone());  // 存储 makespan 供统计使用

            //  f.setObjectives(state, adjustedFitnesses);  // 让 GP 进化使用 ISP 罚值
        }

        if(countInd % state.population.subpops[0].individuals.length == 0) {
            System.out.println("bad run: " + countBadrun);
            countBadrun = 0;
        }

        //modified by fzhang 23.5.2018  save bad run information for one population
        //if(countInd % 1024 == 0) {
   /*     if(countInd % state.population.subpops[0].individuals.length == 0) {
            genNumBadRun.add(countBadrun);
            countBadrun = 0;
           }

        //if(countInd == 1024*51)
        if(countInd == state.population.subpops[0].individuals.length*state.numGenerations)
            WriteCountBadrun(state,null);*/
    }

    //modified by fzhang 26.4.2018   write bad run times to *.csv
/*    public void WriteCountBadrun(EvolutionState state, final Parameter base) {
    	Parameter p;
		// Get the job seed.
		p = new Parameter("seed").push(""+0);
        jobSeed = state.parameters.getLongWithDefault(p, null, 0);
        File countBadRunFile = new File("job." + jobSeed + ".BadRun.csv");

     	try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(countBadRunFile));
			writer.write("generation,numTotalBadRun");
			writer.newLine();
            for(int cutPoint = 0; cutPoint < genNumBadRun.size(); cutPoint++) {
   	 	        writer.write(cutPoint + "," +genNumBadRun.get(cutPoint));
   		    writer.newLine();
            }
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }*/
}
