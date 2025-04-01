package mengxu.algorithm.multipletreegp;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.gp.GPTree;
import ec.util.Checkpoint;
import ec.util.Parameter;
import mengxu.gp.GPRuleEvolutionState;
import mengxu.gp.terminal.JobShopAttribute;
import mengxu.rule.RuleType;
import mengxu.ruleoptimisation.RuleOptimizationProblem;
import mengxu.rule.evolved.GPRule;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static mengxu.gp.terminal.JobShopAttribute.updateActivationProbabilities;

/**
 * The evolution state of evolving dispatching rules with GP.
 *
 * @author yimei
 *
 */

public class GPRuleEvolutionStateBaseline extends GPRuleEvolutionState {

	/**
	 * Read the file to specify the terminals.
	 */
	ArrayList<ArrayList<Double>> storeGenDiversities = new ArrayList<>();
	public List<Individual> ensemble = new ArrayList<>();
	public List<List<Individual>> ensembleAll = new ArrayList<>();

	@Override
	public void setup(EvolutionState state, Parameter base) {

		super.setup(this, base);

	}
	public void updateActivationProbabilities(List<GPRule> bestRules) {
		Map<JobShopAttribute, Integer> sequencingActiveCounts = new HashMap<>();
		Map<JobShopAttribute, Integer> sequencingTotalCounts = new HashMap<>();
		Map<JobShopAttribute, Integer> routingActiveCounts = new HashMap<>();
		Map<JobShopAttribute, Integer> routingTotalCounts = new HashMap<>();
		Map<JobShopAttribute, Integer> sequencingAppearances = new HashMap<>();
		Map<JobShopAttribute, Integer> routingAppearances = new HashMap<>();

		for (GPRule rule : bestRules) {
			RuleType ruleType = rule.getType();  // 获取规则类型

			for (JobShopAttribute attr : rule.getAttributes()) {
				if (ruleType == RuleType.SEQUENCING) {
					sequencingTotalCounts.put(attr, sequencingTotalCounts.getOrDefault(attr, 0) + 1);
					sequencingAppearances.put(attr, sequencingAppearances.getOrDefault(attr, 0) + 1);
					if (Math.random() < (JobShopAttribute.getActivationProbability(attr,ruleType))) {
						sequencingActiveCounts.put(attr, sequencingActiveCounts.getOrDefault(attr, 0) + 1);
					}
				} else if (ruleType == RuleType.ROUTING) {
					routingTotalCounts.put(attr, routingTotalCounts.getOrDefault(attr, 0) + 1);
					routingAppearances.put(attr, routingAppearances.getOrDefault(attr, 0) + 1);
					if (Math.random() < (JobShopAttribute.getActivationProbability(attr,ruleType))) {
						routingActiveCounts.put(attr, routingActiveCounts.getOrDefault(attr, 0) + 1);
					}
				}
			}
		}
//		System.out.println("sequencingActiveCounts: " + sequencingActiveCounts);
//		System.out.println("sequencingTotalCounts: " + sequencingTotalCounts);
//		System.out.println("routingActiveCounts: " + routingActiveCounts);
//		System.out.println("routingActiveCounts: " + routingTotalCounts);
		// ✅ 分别更新 SEQUENCING 和 ROUTING 规则的 AP_j

		//JobShopAttribute.updateActivationProbabilities(routingActiveCounts, routingTotalCounts, RuleType.ROUTING);
		JobShopAttribute.updateActivationProbabilities(routingActiveCounts, routingTotalCounts, RuleType.ROUTING);
		JobShopAttribute.updateActivationProbabilities(sequencingActiveCounts, sequencingTotalCounts, RuleType.SEQUENCING);
	}
	private int countNodes(GPNode node) {
		if (node == null) return 0;
		int count = 1;  // 计入自身节点
		for (GPNode child : node.children) {
			count += countNodes(child);  // 递归计算子节点
		}
		return count;
	}


	public List<GPRule> getBestRulesFromPopulation() {
		List<GPRule> bestRules = new ArrayList<>();
// 获取最佳个体
		for (int i = 0; i < population.subpops.length; i++) {
			//Individual bestIndividual = bestIndi(i);  // 获取最佳个体
			List<Individual> topIndividuals = bestIndis5(i);
			//if (bestIndividual instanceof GPIndividual) {
			for (Individual bestIndividual : topIndividuals) {
				if (bestIndividual instanceof GPIndividual) {
					GPIndividual gpInd = (GPIndividual) bestIndividual;

					if (gpInd.trees.length < 2) {
						System.err.println("Error: GPIndividual does not have both sequencing and routing trees.");
						continue;
					}

					// ✅ 获取 T0 (SEQUENCING) 和 T1 (ROUTING)
					GPTree sequencingTree = gpInd.trees[0];  // T0
					GPTree routingTree = gpInd.trees[1];     // T1
					int seqRuleLength = countNodes(sequencingTree.child);
					int routRuleLength = countNodes(routingTree.child);

					// ✅ 设定规则长度的最大阈值 (可调整)
					int maxSeqLength = 50;  // 例如 50
					int maxRoutLength = 50; // 例如 80

					// ✅ **筛选掉超长规则**
					if (seqRuleLength <= maxSeqLength) {
						bestRules.add(new GPRule(RuleType.SEQUENCING, sequencingTree));
					}
					if (routRuleLength <= maxRoutLength) {
						bestRules.add(new GPRule(RuleType.ROUTING, routingTree));
					}
				} else {
					System.err.println("Error: bestIndi(" + i + ") is not a GPIndividual.");
				}
			}
		}
		return bestRules;
	}
	@Override
	public int evolve() {
	    if (generation > 0)
	        output.message("Generation " + generation);

	    //System.out.println("generation "+generation);
	    // EVALUATION
	    statistics.preEvaluationStatistics(this);

	    evaluator.evaluatePopulation(this);  //// here, after this we evaluate the population
//	    statistics.postEvaluationStatistics(this); //log the best individual



		statistics.postEvaluationStatistics(this); //log the best individual
		//---------------------------------------------------------


		// SHOULD WE QUIT?
	    if (evaluator.runComplete(this) && quitOnRunComplete)
	        {
	        output.message("Found Ideal Individual");
	        return R_SUCCESS;
	        }
	    // SHOULD WE QUIT?
	    if (generation == numGenerations-1)
	        {
	    	generation++; // in this way, the last generation value will be printed properly.  fzhang 28.3.2018
			return R_FAILURE;
	        }

	    // PRE-BREEDING EXCHANGING
	    statistics.prePreBreedingExchangeStatistics(this);
	    population = exchanger.preBreedingExchangePopulation(this);  /** Simply returns state.population. */
	    statistics.postPreBreedingExchangeStatistics(this);

	    String exchangerWantsToShutdown = exchanger.runComplete(this);  /** Always returns null */
	    if (exchangerWantsToShutdown!=null)
	        {
	        output.message(exchangerWantsToShutdown);
	        /*
	         * Don't really know what to return here.  The only place I could
	         * find where runComplete ever returns non-null is
	         * IslandExchange.  However, that can return non-null whether or
	         * not the ideal individual was found (for example, if there was
	         * a communication error with the server).
	         *
	         * Since the original version of this code didn't care, and the
	         * result was initialized to R_SUCCESS before the while loop, I'm
	         * just going to return R_SUCCESS here.
	         */

	        return R_SUCCESS;
	        }

	    // BREEDING
	    statistics.preBreedingStatistics(this);

	    population = breeder.breedPopulation(this); //!!!!!!   return newpop;  if it is NSGA-II, the population here is 2N

	    // POST-BREEDING EXCHANGING
	    statistics.postBreedingStatistics(this);   //position 1  here, a new pop has been generated.

	    // POST-BREEDING EXCHANGING
	    statistics.prePostBreedingExchangeStatistics(this);
	    population = exchanger.postBreedingExchangePopulation(this);   /** Simply returns state.population. */
	    statistics.postPostBreedingExchangeStatistics(this);  //position 2

	    // Generate new instances if needed
		RuleOptimizationProblem problem = (RuleOptimizationProblem)evaluator.p_problem;
	    if (problem.getEvaluationModel().isRotatable()) {
			problem.rotateEvaluationModel();
		}

		// ✅ 获取最佳规则，并更新 `AP_j`
		List<GPRule> bestRules = getBestRulesFromPopulation();
		updateActivationProbabilities(bestRules);

	    // INCREMENT GENERATION AND CHECKPOINT
	    generation++;
	    if (checkpoint && generation%checkpointModulo == 0)
	        {
	        output.message("Checkpointing");
	        statistics.preCheckpointStatistics(this);
	        Checkpoint.setCheckpoint(this);
	        statistics.postCheckpointStatistics(this);
	        }

	    return R_NOTDONE;
	}
	
}
