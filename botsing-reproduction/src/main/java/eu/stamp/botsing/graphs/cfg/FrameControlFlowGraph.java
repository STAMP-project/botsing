package eu.stamp.botsing.graphs.cfg;

import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.RawControlFlowGraph;

public class FrameControlFlowGraph {
    // the original raw cfg
    private RawControlFlowGraph rcfg;
    // The bytecode instruction which is calling the deeper frame method call
    private BytecodeInstruction callingInst;

    public FrameControlFlowGraph(RawControlFlowGraph rcfg, BytecodeInstruction callingInst){
        this.rcfg = rcfg;
        this.callingInst = callingInst;
    }

    public RawControlFlowGraph getRcfg() {
        return rcfg;
    }

    public BytecodeInstruction getCallingInstruction() {
        return callingInst;
    }
}
