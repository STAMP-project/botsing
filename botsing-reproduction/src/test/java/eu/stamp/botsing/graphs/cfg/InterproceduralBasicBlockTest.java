package eu.stamp.botsing.graphs.cfg;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class InterproceduralBasicBlockTest {
    private String  className = "java.lang.Integer";
    private GraphTestingUtils testingUtils = new GraphTestingUtils();
    @Test
    public void test(){
        List<BytecodeInstruction> blockNode= new ArrayList<>();
        BytecodeInstruction stmt1 = testingUtils.mockNewStatement(className,"reverse()");
        BytecodeInstruction stmt2 = testingUtils.mockNewStatement(className,"sum()");
        blockNode.add(stmt1);
        blockNode.add(stmt2);
        InterproceduralBasicBlock interproceduralBasicBlock = new InterproceduralBasicBlock(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT(),className, "sum()",blockNode);

        try{
            interproceduralBasicBlock.appendInstruction(null);
        }catch (IllegalArgumentException e){
            assertTrue(e.getMessage().contains("The given instruction is null"));
        }


        try{
            interproceduralBasicBlock.appendInstruction(stmt1);
        }catch (IllegalArgumentException e){
            assertTrue(e.getMessage().contains("a basic block can not contain the same element twice"));
        }
    }
}
