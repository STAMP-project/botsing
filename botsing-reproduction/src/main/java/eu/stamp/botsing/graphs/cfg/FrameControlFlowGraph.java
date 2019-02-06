package eu.stamp.botsing.graphs.cfg;

import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.RawControlFlowGraph;

public class FrameControlFlowGraph {
    private RawControlFlowGraph rcfg;
    private BytecodeInstruction exitingBCInst;

    public FrameControlFlowGraph(RawControlFlowGraph rcfg, BytecodeInstruction exitingBCInst){
        this.rcfg = rcfg;
        this.exitingBCInst = exitingBCInst;
    }

    public RawControlFlowGraph getRcfg() {
        return rcfg;
    }

    public BytecodeInstruction getExitingBCInst() {
        return exitingBCInst;
    }
}
