package eu.stamp.botsing.graphs.cfg;

import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.ControlFlowEdge;
import org.evosuite.graphs.cfg.RawControlFlowGraph;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Set;

public class GraphTestingUtils {

    public BytecodeInstruction mockNewStatement(String className, String methodName){
        BytecodeInstruction stmt = Mockito.mock(BytecodeInstruction.class);
        Mockito.doReturn(className).when(stmt).getClassName();
        Mockito.doReturn(methodName).when(stmt).getMethodName();
        return stmt;
    }
    public BytecodeInstruction mockNewStatement(String className, String methodName,int lineNumber){
        BytecodeInstruction stmt = Mockito.mock(BytecodeInstruction.class);
        Mockito.doReturn(className).when(stmt).getClassName();
        Mockito.doReturn(methodName).when(stmt).getMethodName();
        Mockito.doReturn(lineNumber).when(stmt).getLineNumber();
        return stmt;
    }

    public void addMockedEdge(BytecodeInstruction[][] edges,RawControlFlowGraph rcfg){
        Set<ControlFlowEdge> edgeSet = new HashSet<>();
        for(int rows=0;rows<edges.length;rows++){
            BytecodeInstruction src =  edges[rows][0];
            BytecodeInstruction target =  edges[rows][1];
            ControlFlowEdge edge = new ControlFlowEdge();
            edgeSet.add(edge);

            Mockito.doReturn(src).when(rcfg).getEdgeSource(edge);
            Mockito.doReturn(target).when(rcfg).getEdgeTarget(edge);
        }
        Mockito.doReturn(edgeSet).when(rcfg).edgeSet();
    }
}
