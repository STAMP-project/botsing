package eu.stamp.botsing.secondaryobjectives.basicblock;

import org.evosuite.graphs.cfg.BasicBlock;

import java.util.Objects;

public class CoveredBasicBlock {

    private BasicBlock basicBlock;
    private boolean fullyCovered;

    public CoveredBasicBlock(BasicBlock basicBlock, boolean fullyCovered){
        this.basicBlock = basicBlock;
        this.fullyCovered = fullyCovered;
    }

    public BasicBlock getBasicBlock() {
        return basicBlock;
    }

    public boolean isFullyCovered() {
        return fullyCovered;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()){
            return false;
        }
        CoveredBasicBlock that = (CoveredBasicBlock) o;

        return fullyCovered == that.fullyCovered &&
                Objects.equals(basicBlock, that.basicBlock);
    }

    @Override
    public int hashCode() {
        return Objects.hash(basicBlock, fullyCovered);
    }
}
