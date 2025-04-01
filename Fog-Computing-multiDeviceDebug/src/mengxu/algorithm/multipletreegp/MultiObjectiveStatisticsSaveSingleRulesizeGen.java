package mengxu.algorithm.multipletreegp;

import ec.EvolutionState;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.multiobjective.MultiObjectiveStatistics;
import ec.multiobjective.MultiObjectiveFitness;
import ec.util.Parameter;
import ec.Individual;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class MultiObjectiveStatisticsSaveSingleRulesizeGen extends MultiObjectiveStatistics {

    // 记录种子值
    protected long jobSeed;

    // 记录每代规则大小
    private final ArrayList<double[]> aveRouRulesizeTree1 = new ArrayList<>();

    /** 在每代评估前执行 */
    public void preEvaluationStatistics(final EvolutionState state) {
        super.preEvaluationStatistics(state);

        // 获取种子
        Parameter p = new Parameter("seed").push("" + 0);
        jobSeed = state.parameters.getLongWithDefault(p, null, 0);

        int numObjectives = ((MultiObjectiveFitness) state.population.subpops[0].individuals[0].fitness).getNumObjectives();
        double[] aveRouSizeTree1 = new double[numObjectives];

        for (int subpop = 0; subpop < state.population.subpops.length; subpop++) {
            double[] RouSizeTree1 = new double[numObjectives];

            for (int inds = 0; inds < state.population.subpops[subpop].individuals.length; inds++) {
                GPIndividual indi = (GPIndividual) state.population.subpops[subpop].individuals[inds];

                for (int obj = 0; obj < numObjectives; obj++) {
                    RouSizeTree1[obj] += indi.trees[0].child.numNodes(GPNode.NODESEARCH_ALL);
                }
            }

            for (int obj = 0; obj < numObjectives; obj++) {
                aveRouSizeTree1[obj] = RouSizeTree1[obj] / state.population.subpops[subpop].individuals.length;
            }
            aveRouRulesizeTree1.add(aveRouSizeTree1.clone());
        }

        // 只有在最后一代才写入文件
        if (state.generation == state.numGenerations - 1) {
            saveRulesizeToFile();
        }
    }

    /** 记录最终的 Pareto Front，并保存到文件 */
    public void finalStatistics(final EvolutionState state, final int result) {
        super.finalStatistics(state, result);
        saveParetoFrontToFile(state);
    }

    /** 输出规则大小到 CSV 文件 */
    private void saveRulesizeToFile() {
        File rulesizeFile = new File("job." + jobSeed + ".aveGenRulesize.csv");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(rulesizeFile))) {
            // 写入 CSV 表头
            writer.write("Gen");
            int numObjectives = aveRouRulesizeTree1.get(0).length;
            for (int obj = 0; obj < numObjectives; obj++) {
                writer.write(",Objective_" + obj + "_RouRuleSize");
            }
            writer.newLine();

            // 写入数据
            for (int gen = 0; gen < aveRouRulesizeTree1.size(); gen++) {
                writer.write(gen + "");
                for (int obj = 0; obj < numObjectives; obj++) {
                    writer.write("," + aveRouRulesizeTree1.get(gen)[obj]);
                }
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** 保存 Pareto 前沿数据 */
    private void saveParetoFrontToFile(EvolutionState state) {
        for (int s = 0; s < state.population.subpops.length; s++) {
            MultiObjectiveFitness typicalFitness = (MultiObjectiveFitness) (state.population.subpops[s].individuals[0].fitness);
            ArrayList<Individual> front = typicalFitness.partitionIntoParetoFront(state.population.subpops[s].individuals, null, null);

            File paretoFile = new File("job." + jobSeed + ".paretoFront.subpop" + s + ".csv");

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(paretoFile))) {
                int numObjectives = ((MultiObjectiveFitness) front.get(0).fitness).getNumObjectives();

                writer.write("Individual");
                for (int obj = 0; obj < numObjectives; obj++) {
                    writer.write(",Objective_" + obj);
                }
                writer.newLine();

                for (int i = 0; i < front.size(); i++) {
                    Individual ind = front.get(i);
                    MultiObjectiveFitness mof = (MultiObjectiveFitness) (ind.fitness);
                    writer.write("Ind_" + i);
                    for (int obj = 0; obj < numObjectives; obj++) {
                        writer.write("," + mof.getObjective(obj));
                    }
                    writer.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
