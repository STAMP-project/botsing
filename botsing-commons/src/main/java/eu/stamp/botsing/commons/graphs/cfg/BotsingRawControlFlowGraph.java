package eu.stamp.botsing.commons.graphs.cfg;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import eu.stamp.botsing.commons.instrumentation.InstrumentingClassLoader;
import org.evosuite.shaded.org.apache.commons.collections.map.HashedMap;
import org.evosuite.graphs.cfg.*;
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
                LOG.debug("{} is a THROW. We omitted it for now",exitPoint);
                continue;
            }
            if(!alreadyRedirected){
                for(ControlFlowEdge outgoingEdge: outgoingEdgesOfSrc){
                    this.redirectEdgeSource(outgoingEdge,exitPoint);
                }
                alreadyRedirected = true;
            }else{
                for(ControlFlowEdge outgoingEdge: outgoingEdgesOfSrc){
                    this.addEdge(exitPoint,this.getEdgeTarget(outgoingEdge));
                }
            }
        }
        if(!alreadyRedirected){
            LOG.warn("method "+target.getMethodName()+" does not have any exit point");
        }
        this.addEdge(src, target, false);
    }

    public void addGeneralEntryPoint(BytecodeInstruction src){
        for(BytecodeInstruction target: this.determineEntryPoints()){
            if(!target.equals(src)){
                this.addEdge(src, target, false);
            }
        }
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

    public void cloneAsNewNode(BytecodeInstruction source, RawControlFlowGraph cfg) {
        BotsingRawControlFlowGraph tempCFG = new BotsingRawControlFlowGraph(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),"IntegrationTestingGraph","methodsIntegration",1);
        Map<Integer,Integer> newNodes = new HashedMap();
        int id = BytecodeInstructionPool.getInstance(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT()).getInstructionsIn(cfg.getClassName(),cfg.getMethodName()).size();
        // clone nodes
        Set<BytecodeInstruction> vertexes = cfg.vertexSet();
        for (BytecodeInstruction ins : vertexes) {
            InstrumentingClassLoader classLoader = BotsingTestGenerationContext.getInstance().getClassLoaderForSUT();

            BytecodeInstruction bc = new BytecodeInstruction(classLoader,cfg.getClassName(),cfg.getMethodName(),id,0,ins.getASMNode(),ins.getLineNumber(),ins.getBasicBlock());
            tempCFG.addVertex(bc);
            newNodes.put(ins.getInstructionId(),id);
            this.addVertex(bc);
            id++;
        }


        // clone edges
        Set<ControlFlowEdge> edges = cfg.edgeSet();
        for (ControlFlowEdge edge : edges) {
            BytecodeInstruction src = cfg.getEdgeSource(edge);
            BytecodeInstruction target = cfg.getEdgeTarget(edge);

            BytecodeInstruction newSrc = tempCFG.getInstruction(newNodes.get(src.getInstructionId()));
            BytecodeInstruction newTarget = tempCFG.getInstruction(newNodes.get(target.getInstructionId()));
            tempCFG.addEdges(newSrc, newTarget, edge.isExceptionEdge());
            this.addEdge(newSrc, newTarget, edge.isExceptionEdge());
        }
        // Add interProceduralCFG
        BytecodeInstruction newTarget = tempCFG.determineEntryPoint();
        Set<BytecodeInstruction> newExitPoint = tempCFG.determineExitPoints();
        this.addInterProceduralEdge(source,newTarget,newExitPoint);
    }

    public void addEdges(BytecodeInstruction src,BytecodeInstruction target, boolean isException){
        this.addEdge(src, target, isException);
    }
}
