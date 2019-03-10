package eu.stamp.botsing.graphs.cfg;

import org.evosuite.graphs.cfg.BasicBlock;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.ControlFlowEdge;
import org.evosuite.graphs.cfg.RawControlFlowGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class BotsingRawControlFlowGraph extends RawControlFlowGraph {
    private static Logger LOG = LoggerFactory.getLogger(BotsingRawControlFlowGraph.class);

    public BotsingRawControlFlowGraph(ClassLoader classLoader, String className, String methodName, int access) {
        super(classLoader, className, methodName, access);
    }


    public void clone(RawControlFlowGraph cfg) {
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
            this.addEdge(src, target, edge.isExceptionEdge());
        }
    }

    public void addInterProceduralEdge(BytecodeInstruction src, BytecodeInstruction target, Set<BytecodeInstruction> targetGraphExitPoints) {
        Set<ControlFlowEdge> outgoingEdgesOfSrc = this.outgoingEdgesOf(src);
        boolean alreadyRedirected = false;
        for(BytecodeInstruction exitPoint : targetGraphExitPoints){
            if(exitPoint.isThrow()){
                LOG.info("{} is a THROW",exitPoint);
                continue;
            }
            if(!alreadyRedirected){
                for(ControlFlowEdge outgoingEdge: outgoingEdgesOfSrc){
                    this.redirectEdgeSource(outgoingEdge,exitPoint);
                }
                alreadyRedirected = true;
            }else{
                for(ControlFlowEdge outgoingEdge: outgoingEdgesOfSrc){
                    this.addEdge(exitPoint,this.getEdgeTarget(outgoingEdge),outgoingEdge);
                }
            }
        }
        if(!alreadyRedirected){
            LOG.warn("There is no exit point for the target method");
            for(ControlFlowEdge outgoingEdge: outgoingEdgesOfSrc){
                this.redirectEdgeSource(outgoingEdge,null);
            }
        }
        this.addEdge(src, target, false);
    }

    @Override
    public BasicBlock determineBasicBlockFor(BytecodeInstruction instruction) {
        if (instruction == null) {
            throw new IllegalArgumentException("The given bytecode instruction is null");
        }

        List<BytecodeInstruction> blockNodes = new ArrayList<BytecodeInstruction>();
        blockNodes.add(instruction);

        Set<BytecodeInstruction> handledChildren = new HashSet<BytecodeInstruction>();
        Set<BytecodeInstruction> handledParents = new HashSet<BytecodeInstruction>();

        Queue<BytecodeInstruction> queue = new LinkedList<BytecodeInstruction>();
        queue.add(instruction);
        while (!queue.isEmpty()) {
            BytecodeInstruction currentInstruction = queue.poll();
            // Add examined child
            // it should be only one child
            if (outDegreeOf(currentInstruction) == 1) {
                BytecodeInstruction child = getChildren(currentInstruction).iterator().next();
                if (!blockNodes.contains(child) && !handledChildren.contains(child)) {
                    handledChildren.add(child);
                    if (inDegreeOf(child) < 2) {
                        blockNodes.add(blockNodes.indexOf(currentInstruction) + 1, child);
                        queue.add(child);
                    }
                }
            }

            // Add examined child
            // it should be only one parent
            if (inDegreeOf(currentInstruction) == 1) {
                BytecodeInstruction parent = getParents(currentInstruction).iterator().next();
                if (!blockNodes.contains(parent) && !handledParents.contains(parent)) {
                    handledParents.add(parent);
                    if (outDegreeOf(parent) < 2) {
                        // insert parent before current
                        blockNodes.add(blockNodes.indexOf(currentInstruction), parent);
                        queue.add(parent);
                    }
                }
            }
        }
        InterproceduralBasicBlock basicBlock = new InterproceduralBasicBlock(this.getClassLoader(),this.className,this.methodName,blockNodes);
        LOG.debug("Created basic block: {}",basicBlock.toString());
        return basicBlock;
    }

//    @Override
//    protected ControlFlowEdge addEdge(BytecodeInstruction src, BytecodeInstruction target, boolean isExceptionEdge) {
//        LOG.debug("Adding edge to RawCFG of " + this.className + "." + this.methodName + ": " + this.vertexCount());
//        if (BranchPool.getInstance(this.classLoader).isKnownAsBranch(src)) {
//            if (src.isBranch()) {
//                return this.addBranchEdge(src, target, isExceptionEdge);
//            }
//
//            if (src.isSwitch()) {
//                return this.addSwitchBranchEdge(src, target, isExceptionEdge);
//            }
//        }
//
//        return this.addUnlabeledEdge(src, target, isExceptionEdge);
//    }
}
