package eu.stamp.botsing.ga.strategy.mosa.structural;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.botsing.commons.coverage.branch.IntegrationTestingBranchCoverageGoal;
import eu.stamp.botsing.commons.coverage.branch.IntegrationTestingBranchCoverageTestFitness;
import org.evosuite.coverage.branch.Branch;

import org.evosuite.ga.Chromosome;
import org.evosuite.graphs.GraphPool;
import org.evosuite.graphs.cfg.ActualControlFlowGraph;
import org.evosuite.graphs.cfg.BasicBlock;
import org.evosuite.testcase.TestFitnessFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class BranchFitnessGraph<T extends Chromosome, V extends TestFitnessFunction> extends org.evosuite.ga.metaheuristics.mosa.structural.BranchFitnessGraph {
    private static final Logger LOG = LoggerFactory.getLogger(BranchFitnessGraph.class);
    public BranchFitnessGraph(Set goals) {
        super(goals);
    }


    @Override
    public void deriveDependencies(Set goals) {


        // derive dependencies among branches
        for (TestFitnessFunction fitness : (Set<TestFitnessFunction>) goals){
            Branch branch = ((IntegrationTestingBranchCoverageTestFitness) fitness).getBranch();
            if (branch==null){
                this.rootBranches.add(fitness);
                continue;
            }

            if (branch.getInstruction().isRootBranchDependent()){
                this.rootBranches.add(fitness);
            }

            // see dependencies for all true/false branches
            ActualControlFlowGraph rcfg = GraphPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).getActualCFG("IntegrationTestingGraph","methodsIntegration");
            Set<BasicBlock> visitedBlock = new HashSet<BasicBlock>();
            Set<BasicBlock> parents = lookForParent(branch.getInstruction().getBasicBlock(), rcfg, visitedBlock);
            for (BasicBlock bb : parents){
                Branch newB = extractBranch(bb);
                if (newB == null){
                    this.rootBranches.add(fitness);
                    continue;
                }

                // ToDo: We should select only one of them for crash reproduction.

                IntegrationTestingBranchCoverageGoal goal = new IntegrationTestingBranchCoverageGoal(newB, true, newB.getClassName(), newB.getMethodName());
                IntegrationTestingBranchCoverageTestFitness newFitness = new IntegrationTestingBranchCoverageTestFitness(goal);
                IntegrationTestingBranchCoverageGoal goal2 = new IntegrationTestingBranchCoverageGoal(newB, false, newB.getClassName(), newB.getMethodName());
                IntegrationTestingBranchCoverageTestFitness newfitness2 = new IntegrationTestingBranchCoverageTestFitness(goal2);
                if(graph.containsVertex(newFitness) && graph.containsVertex(newfitness2)){
                    throw new IllegalStateException("branches in the graph are not right!");
                }else if(graph.containsVertex(newFitness)){
                    graph.addEdge( newFitness, fitness);
                }else if(graph.containsVertex(newfitness2)){
                    graph.addEdge( newfitness2, fitness);
                }else{
                    LOG.info("Removed vertex: {}",newFitness);
                }
            }
        }
    }
}
