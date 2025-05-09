/*
  Copyright 2006 by Sean Luke
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package ec.multiobjective;

import java.io.*;
import ec.util.DecodeReturn;
import ec.util.Parameter;
import ec.util.Code;
import ec.Fitness;
import ec.EvolutionState;
import java.util.*;
import ec.*;


public class MultiObjectiveFitness extends Fitness
    {
    public static final String MULTI_FITNESS_POSTAMBLE = "[";
    public static final String FITNESS_POSTAMBLE = "]";

    /** parameter for size of objectives */
    public static final String P_NUMOBJECTIVES = "num-objectives";

    /** parameter for max fitness values */
    public static final String P_MAXOBJECTIVES = "max";

    /** parameter for min fitness values */
    public static final String P_MINOBJECTIVES = "min";

    /** Is higher better? */
    public static final String P_MAXIMIZE = "maximize";

    /** Desired maximum fitness values. By default these are 1.0. Shared. */
    public double[] maxObjective;

    /** Desired minimum fitness values. By default these are 0.0. Shared. */
    public double[] minObjective;

    /** Maximization.  Shared. */
    public boolean[] maximize;

    /** The various fitnesses. */
    public double[] objectives; // values range from 0 (worst) to 1 INCLUSIVE

    /** Returns auxilliary fitness value names to be printed by the statistics object.
        By default, an empty array is returned, but various algorithms may override this to provide additional columns.
    */
    public String[] getAuxilliaryFitnessNames() { return new String[] { }; }

    /** Returns auxilliary fitness values to be printed by the statistics object.
        By default, an empty array is returned, but various algorithms may override this to provide additional columns.
    */
    public double[] getAuxilliaryFitnessValues() { return new double[] { }; }

    /**
        @deprecated Use isMaximizing(objective).  This function now just returns whether the first objective is maximizing.
    */
    public boolean isMaximizing()
        {
            System.out.println("1111"+maximize[0]);
        return maximize[0];
        }

    public boolean isMaximizing(int objective)
        {
            System.out.println("222222222222222"+ maximize[objective]);
        return maximize[objective];
        }


    public int getNumObjectives() { return objectives.length; }

    /**
     * Returns the objectives as an array. Note that this is the *actual array*.
     * Though you could set values in this array, you should NOT do this --
     * rather, set them using setObjectives().
     */
    public double[] getObjectives()
        {
        return objectives;
        }

    public double getObjective(int i)
        {
        return objectives[i];
        }

    public void setObjectives(final EvolutionState state, double[] newObjectives)
        {
        if (newObjectives == null)
            {
            state.output.fatal("Null objective array provided to MultiObjectiveFitness.");
            }
            objectives = newObjectives;
 

        }

    public Parameter defaultBase()
        {
        return MultiObjectiveDefaults.base().push(P_FITNESS);
        }

    public Object clone()
        {
        MultiObjectiveFitness f = (MultiObjectiveFitness) (super.clone());
        f.objectives = (double[]) (objectives.clone()); // cloning an array
        // note that we do NOT clone max and min fitness, or maximizing -- they're shared
        return f;
        }

    /**
     * Returns the Max() of objectives, which adheres to Fitness.java's protocol
     * for this method. Though you should not rely on a selection or statistics
     * method which requires this.
     */
    public double fitness()
        {
        double fit = objectives[0];
        for (int x = 1; x < objectives.length; x++)
            if (fit < objectives[x])
                fit = objectives[x];
        return fit;
        }

    /**
     * Sets up. This must be called at least once in the prototype before
     * instantiating any fitnesses that will actually be used in evolution.
     */

    public void setup(EvolutionState state, Parameter base)
        {
        super.setup(state, base); // unnecessary really

        Parameter def = defaultBase();
        int numFitnesses;

        numFitnesses = state.parameters.getInt(base.push(P_NUMOBJECTIVES), def.push(P_NUMOBJECTIVES), 0);
        if (numFitnesses <= 0)
            state.output.fatal("The number of objectives must be an integer >= 1.", base.push(P_NUMOBJECTIVES), def.push(P_NUMOBJECTIVES));

        objectives = new double[numFitnesses];
        maxObjective = new double[numFitnesses];
        minObjective = new double[numFitnesses];
        maximize = new boolean[numFitnesses];

        for (int i = 0; i < numFitnesses; i++)
            {
            // load default globals
            minObjective[i] = state.parameters.getDoubleWithDefault(base.push(P_MINOBJECTIVES), def.push(P_MINOBJECTIVES), 0.0);
            maxObjective[i] = state.parameters.getDoubleWithDefault(base.push(P_MAXOBJECTIVES), def.push(P_MAXOBJECTIVES), 1.0);
            maximize[i] = state.parameters.getBoolean(base.push(P_MAXIMIZE), def.push(P_MAXIMIZE), true);

            // load specifics if any
            minObjective[i] = state.parameters.getDoubleWithDefault(base.push(P_MINOBJECTIVES).push("" + i), def.push(P_MINOBJECTIVES).push("" + i), minObjective[i]);
            maxObjective[i] = state.parameters.getDoubleWithDefault(base.push(P_MAXOBJECTIVES).push("" + i), def.push(P_MAXOBJECTIVES).push("" + i), maxObjective[i]);
            maximize[i] = state.parameters.getBoolean(base.push(P_MAXIMIZE).push("" + i), def.push(P_MAXIMIZE).push("" + i), maximize[i]);

            // test for validity
            if (minObjective[i] >= maxObjective[i])
                state.output.error("For objective " + i + "the min fitness must be strictly less than the max fitness.");
            }
        state.output.exitIfErrors();
        }

    /**
     * Returns true if this fitness is the "ideal" fitness. Default always
     * returns false. You may want to override this.
     */
    public boolean isIdealFitness()
        {
        return false;
        }

    /**
     * Returns true if I'm equivalent in fitness (neither better nor worse) to
     * _fitness. The rule I'm using is this: If one of us is better in one or
     * more criteria, and we are equal in the others, then equivalentTo is
     * false. If each of us is better in one or more criteria each, or we are
     * equal in all criteria, then equivalentTo is true.   Multiobjective optimization algorithms may
     * choose to override this to do something else.
     */

    public boolean equivalentTo(Fitness _fitness)
        {
        MultiObjectiveFitness other = (MultiObjectiveFitness) _fitness;
        boolean abeatsb = false;
        boolean bbeatsa = false;
        if (objectives.length != other.objectives.length)
            throw new RuntimeException("Attempt made to compare two multiobjective fitnesses; but they have different numbers of objectives.");
        System.out.println("objectives.length"+objectives.length);
            System.out.println(maximize);
        for (int x = 0; x < objectives.length; x++)
            {
            if (maximize[x] != other.maximize[x])  // uh oh
                throw new RuntimeException(
                    "Attempt made to compare two multiobjective fitnesses; but for objective #" + x +
                    ", one expects higher values to be better and the other expectes lower values to be better.");

            if (maximize[x])
                {
                if (objectives[x] > other.objectives[x])
                    abeatsb = true;
                if (objectives[x] < other.objectives[x])
                    bbeatsa = true;
                if (abeatsb && bbeatsa)
                    return true;
                }
            else
                {
                if (objectives[x] < other.objectives[x])
                    abeatsb = true;
                if (objectives[x] > other.objectives[x])
                    bbeatsa = true;
                if (abeatsb && bbeatsa)
                    return true;
                }
            }
        if (abeatsb || bbeatsa)
            return false;
        return true;
        }

    /**
     * Returns true if I'm better than _fitness. The DEFAULT rule I'm using is this: if
     * I am better in one or more criteria, and we are equal in the others, then
     * betterThan is true, else it is false. Multiobjective optimization algorithms may
     * choose to override this to do something else.
     */

    public boolean betterThan(Fitness fitness)
        {
        return paretoDominates((MultiObjectiveFitness)fitness);
        }

    /**
     * Returns true if I'm better than _fitness. The rule I'm using is this: if
     * I am better in one or more criteria, and we are equal in the others, then
     * betterThan is true, else it is false.
     */

    public boolean paretoDominates(MultiObjectiveFitness other)
        {
        boolean abeatsb = false;
        if (objectives.length != other.objectives.length)
            throw new RuntimeException("Attempt made to compare two multiobjective fitnesses; but they have different numbers of objectives.");
        for (int x = 0; x < objectives.length; x++)
            {
            if (maximize[x] != other.maximize[x])  // uh oh
                throw new RuntimeException(
                    "Attempt made to compare two multiobjective fitnesses; but for objective #" + x +
                    ", one expects higher values to be better and the other expectes lower values to be better.");

            if (maximize[x]) //fzhang 2018.11.2  maximize[0] and maximize[1] are false. It means we are looking at minimising.
                {
                if (objectives[x] > other.objectives[x])
                    abeatsb = true;
                else if (objectives[x] < other.objectives[x])
                    return false;
                }
            else
                {
                if (objectives[x] < other.objectives[x]) //objective[0] and objective[1]: [2514.5953504074932, 498.03951619189513]
                    abeatsb = true;
                else if (objectives[x] > other.objectives[x])
                    return false;
                }
            }

        return abeatsb;
        }

    // Remove an individual from the ArrayList, shifting the topmost
    // individual in his place
    static void yank(int val, ArrayList list)
        {
        int size = list.size();
        list.set(val, list.get(size - 1));
        list.remove(size - 1);
        }

    /**
     * Divides an array of Individuals into the Pareto front and the "nonFront" (everyone else).
     * The Pareto front is returned.  You may provide ArrayLists for the front and a nonFront.
     * If you provide null for the front, an ArrayList will be created for you.  If you provide
     * null for the nonFront, non-front individuals will not be added to it.  This algorithm is
     * O(n^2).
     */
    public static ArrayList partitionIntoParetoFront(Individual[] inds, ArrayList front, ArrayList nonFront)
        {
        if (front == null)
            front = new ArrayList();

        // put the first guy in the front
        front.add(inds[0]);

        // iterate over all the remaining individuals
        for (int i = 1; i < inds.length; i++)
            {
            Individual ind = (Individual) (inds[i]);

            boolean noOneWasBetter = true;
            int frontSize = front.size();

            // iterate over the entire front
            for (int j = 0; j < frontSize; j++)
                {
                Individual frontmember = (Individual) (front.get(j));

                // if the front member is better than the individual, dump the individual and go to the next one
                if (((MultiObjectiveFitness) (frontmember.fitness)).paretoDominates((MultiObjectiveFitness) (ind.fitness)))
                    {
                    if (nonFront != null) nonFront.add(ind);
                    noOneWasBetter = false;
                    break;  // failed.  He's not in the front
                    }
                // if the individual was better than the front member, dump the front member.  But look over the
                // other front members (don't break) because others might be dominated by the individual as well.
                else if (((MultiObjectiveFitness) (ind.fitness)).paretoDominates((MultiObjectiveFitness) (frontmember.fitness)))
                    {
                    yank(j, front);
                    // a front member is dominated by the new individual.  Replace him
                    frontSize--; // member got removed
                    j--;  // because there's another guy we now need to consider in his place
                    if (nonFront != null) nonFront.add(frontmember);
                    }
                }
            if (noOneWasBetter)
                front.add(ind);
            }
        return front;
        }


    /** Divides inds into pareto front ranks (each an ArrayList), and returns them, in order,
        stored in an ArrayList. */
    public static ArrayList partitionIntoRanks(Individual[] inds)
        {
        Individual[] dummy = new Individual[0];
        ArrayList frontsByRank = new ArrayList();//each front is stored by one arraylist

        while(inds.length > 0) //the dominated individuals
            {
            ArrayList front = new ArrayList();
            ArrayList nonFront = new ArrayList();
            MultiObjectiveFitness.partitionIntoParetoFront(inds, front, nonFront); //after this, the individuals are divided into two groups: front and nonfront

            // build inds out of remainder
            inds = (Individual[]) nonFront.toArray(dummy);//copy the non-dominated individuals into inds
            frontsByRank.add(front); //add the non-dominated individuals into frontsByRank
            }
        return frontsByRank;
        }


    /** Returns the Pareto rank for each individual.  Rank 0 is the best rank, then rank 1, and so on.  This is O(n) but it has a high constant overhead because it
        allocates a hashmap and does some autoboxing. */
    public static int[] getRankings(Individual[] inds)
        {
        int[] r = new int[inds.length];
        ArrayList ranks = partitionIntoRanks(inds);  // get all the ranks

        // build a mapping of Individual -> index in inds array
        HashMap m = new HashMap();
        for(int i = 0; i < inds.length; i++)
            m.put(inds[i], Integer.valueOf(i));

        int numRanks = ranks.size();
        for(int rank = 0 ; rank < numRanks; rank++)  // for each rank...
            {
            ArrayList front = (ArrayList)(ranks.get(rank));
            int numInds = front.size();
            for(int ind = 0; ind < numInds; ind++)  // for each individual in that rank ...
                {
                // get the index of the individual in the inds array
                int i = ((Integer)(m.get(front.get(ind)))).intValue();
                r[i] = rank;  // set the rank in the corresponding ranks array
                }
            }
        return r;
        }



    /**
     * Returns the sum of the squared difference between two Fitnesses in Objective space.
     */
    public double sumSquaredObjectiveDistance(MultiObjectiveFitness other)
        {
        double s = 0;
        for (int i = 0; i < objectives.length; i++)
            {
            double a = (objectives[i] - other.objectives[i]);
            s += a * a;
            }
        return s;
        }


    /**
     * Returns the Manhattan difference between two Fitnesses in Objective space.
     */
    public double manhattanObjectiveDistance(MultiObjectiveFitness other)
        {
        double s = 0;
        for (int i = 0; i < objectives.length; i++)
            {
            s += Math.abs(objectives[i] - other.objectives[i]);
            }
        return s;
        }


    public String fitnessToString()
        {
        String s = FITNESS_PREAMBLE + MULTI_FITNESS_POSTAMBLE;
        for (int x = 0; x < objectives.length; x++)
            {
            if (x > 0)
                s = s + " ";
            s = s + Code.encode(objectives[x]);
            }
        return s + FITNESS_POSTAMBLE;
        }


    public String fitnessToStringForHumans()
        {
        String s = FITNESS_PREAMBLE + MULTI_FITNESS_POSTAMBLE;
        for (int x = 0; x < objectives.length; x++)
            {
            if (x > 0)
                s = s + " ";
            s = s + objectives[x];
            }
        return s + FITNESS_POSTAMBLE;
        }

    public void readFitness(final EvolutionState state, final LineNumberReader reader) throws IOException
        {
        DecodeReturn d = Code.checkPreamble(FITNESS_PREAMBLE + MULTI_FITNESS_POSTAMBLE, state, reader);
        for (int x = 0; x < objectives.length; x++)
            {
            Code.decode(d);
            if (d.type != DecodeReturn.T_DOUBLE)
                state.output.fatal("Reading Line " + d.lineNumber + ": " + "Bad Fitness (objectives value #" + x + ").");
            objectives[x] = (double) d.d;
            }
        }

    public void writeFitness(final EvolutionState state, final DataOutput dataOutput) throws IOException
        {
        dataOutput.writeInt(objectives.length);
        for (int x = 0; x < objectives.length; x++)
            dataOutput.writeDouble(objectives[x]);
        writeTrials(state, dataOutput);
        }

    public void readFitness(final EvolutionState state, final DataInput dataInput) throws IOException
        {
        int len = dataInput.readInt();
        if (objectives == null || objectives.length != len)
            objectives = new double[len];
        for (int x = 0; x < objectives.length; x++)
            objectives[x] = dataInput.readDouble();
        readTrials(state, dataInput);
        }


    public void setToBestOf(EvolutionState state, Fitness[] fitnesses)
        {
        state.output.fatal("setToBestOf(EvolutionState, Fitness[]) not implemented in " + this.getClass());
        }

    public void setToMeanOf(EvolutionState state, Fitness[] fitnesses)
        {
        // basically we compute the centroid of the fitnesses
        double sum = 0.0;
        for(int i = 0; i < objectives.length; i++)
            {
            for(int k = 0; k < fitnesses.length; k++)
                {
                MultiObjectiveFitness f = (MultiObjectiveFitness) fitnesses[k];
                sum += f.objectives[i];
                }
            objectives[i] = (double)(sum / fitnesses.length);
            }
        }

    public void setToMedianOf(EvolutionState state, Fitness[] fitnesses)
        {
        state.output.fatal("setToMedianOf(EvolutionState, Fitness[]) not implemented in " + this.getClass());
        }
    }
