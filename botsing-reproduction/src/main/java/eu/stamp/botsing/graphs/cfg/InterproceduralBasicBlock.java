package eu.stamp.botsing.graphs.cfg;

import org.evosuite.graphs.cfg.BasicBlock;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class InterproceduralBasicBlock extends BasicBlock {
    private static Logger LOG = LoggerFactory.getLogger(InterproceduralBasicBlock.class);
    public InterproceduralBasicBlock(ClassLoader classLoader, String className, String methodName, List<BytecodeInstruction> blockNodes) {
        super(classLoader, className, methodName, blockNodes);
    }

    @Override
    protected boolean appendInstruction(BytecodeInstruction instruction){
        if (instruction == null){
            throw new IllegalArgumentException("null given");
        }

        if (instruction.hasBasicBlockSet()){
            LOG.info("existing basic block: {}", instruction.getBasicBlock().explain());
            LOG.info("current basic block: {}", this.explain());
            LOG.info("DUPLICATE BASIC BLOCK: {} {} {} -- {}", instruction.getClassName(), instruction.getMethodName(),instruction.getLineNumber(),instruction.explain());
//            throw new IllegalArgumentException("expect to get instruction "+instruction.explain()+" without. BasicBlock already set to: "+instruction.getBasicBlock().explain());
        }
        if (this.instructions.contains(instruction)){
            throw new IllegalArgumentException("a basic block can not contain the same element twice");
        }
//        LOG.info("--> {}",instruction.explain());
//        if(!instruction.hasBasicBlockSet())
            instruction.basicBlock = this;
//        instruction.setBasicBlock(this);

        return this.instructions.add(instruction);
    }
}
