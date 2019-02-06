package eu.stamp.botsing.graphs.cfg;

import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.ControlFlowEdge;
import org.evosuite.graphs.cfg.RawControlFlowGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class BotsingControlFlowGraph extends RawControlFlowGraph {
    private static Logger LOG = LoggerFactory.getLogger(BotsingControlFlowGraph.class);

    public BotsingControlFlowGraph(ClassLoader classLoader, String className, String methodName, int access) {
        super(classLoader, className, methodName, access);
    }


    public void clone(RawControlFlowGraph cfg){
        Set<BytecodeInstruction> vertexes = cfg.vertexSet();
        cloneVertexes(vertexes);
        cloneEdges(cfg);
    }

    private void cloneVertexes(Set<BytecodeInstruction> vertexes) {
        for (BytecodeInstruction ins : vertexes) {
            this.addVertex(ins);
        }
    }


    private void cloneEdges(RawControlFlowGraph cfg) {
        Set<ControlFlowEdge> edges = cfg.edgeSet();
        for (ControlFlowEdge edge : edges) {
            BytecodeInstruction src = cfg.getEdgeSource(edge);
            BytecodeInstruction target = cfg.getEdgeTarget(edge);
            this.addEdge(src,target,edge.isExceptionEdge());
        }
    }

    public void addInterProceduralEdge(BytecodeInstruction src, BytecodeInstruction target){
        this.addVertex(target);
        this.addEdge(src,target,false);
    }
}
