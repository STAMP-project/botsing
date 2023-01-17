package eu.stamp.botsing.commons.graphs.cfg;

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
            throw new IllegalArgumentException("The given instruction is null");
        }

        if (instruction.hasBasicBlockSet()){
            LOG.debug("existing basic block: {}", instruction.getBasicBlock().explain());
            LOG.debug("current basic block: {}", this.explain());
            LOG.debug("DUPLICATE BASIC BLOCK: {} {} {} -- {}", instruction.getClassName(), instruction.getMethodName(),instruction.getLineNumber(),instruction.explain());
        }
        if (this.instructions.contains(instruction)){
            throw new IllegalArgumentException("a basic block can not contain the same element twice");
        }

        instruction.setBasicBlock(this);
        return this.instructions.add(instruction);
    }
}
