package eu.stamp.botsing.commons.graphs.cfg;

import org.evosuite.graphs.cfg.ActualControlFlowGraph;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.RawControlFlowGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class BotsingActualControlFlowGraph extends ActualControlFlowGraph {
    private static Logger LOG = LoggerFactory.getLogger(BotsingActualControlFlowGraph.class);

    public BotsingActualControlFlowGraph(RawControlFlowGraph rawGraph) {
        super(rawGraph);
    }

    @Override
    protected void setEntryPoint(BytecodeInstruction entryPoint) {
        if (entryPoint == null) {
            throw new IllegalArgumentException("The given entryPoint is null");
        }
        this.entryPoint = entryPoint;
    }

    @Override
    protected void setExitPoints(Set<BytecodeInstruction> exitPoints) {
        if (exitPoints == null) {
            throw new IllegalArgumentException("The given exitPoints set is null");
        }
        this.exitPoints = new HashSet<BytecodeInstruction>();
        for (BytecodeInstruction exitPoint : exitPoints) {
            if (!exitPoint.canBeExitPoint()){
                throw new IllegalArgumentException("unexpected exitPoint byteCode instruction type: " + exitPoint.getInstructionType());
            }
            this.exitPoints.add(exitPoint);
        }
    }

    @Override
    protected void setBranches(Set<BytecodeInstruction> branches) {
        if (branches == null){
            throw new IllegalArgumentException("The given branches set is null");
        }
        this.branches = new HashSet<BytecodeInstruction>();

        for (BytecodeInstruction branch : branches) {
            this.branches.add(branch);
        }
    }

    @Override
    protected void setJoins(Set<BytecodeInstruction> joins){

        if (joins == null){
            throw new IllegalArgumentException("The given joins set is null");
        }
        this.joins = new HashSet<BytecodeInstruction>();

        for (BytecodeInstruction join : joins) {
            this.joins.add(join);
        }
    }
}
