package eu.stamp.cling.coverage.branch;

import org.evosuite.coverage.branch.Branch;
import org.evosuite.graphs.cfg.BytecodeInstruction;

public class BranchPair {

    private Branch firstBranch;
    private Branch secondBranch;
    private BytecodeInstruction callSite;
    private Boolean expression = null;

    public BranchPair(Branch firstBranch,Branch secondBranch, BytecodeInstruction callSite){
        this.firstBranch = firstBranch;
        this.secondBranch = secondBranch;
        this.callSite = callSite;
    }

    public BranchPair(Branch firstBranch,Branch secondBranch, BytecodeInstruction callSite, boolean expression){
        this.firstBranch = firstBranch;
        this.secondBranch = secondBranch;
        this.callSite = callSite;
        this.expression = expression;
    }

    public Branch getFirstBranch() {
        return firstBranch;
    }

    public Branch getSecondBranch() {
        return secondBranch;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null){
            return false;
        }

        if(obj instanceof BranchPair){
            return (((BranchPair) obj).getFirstBranch().equals(this.firstBranch) && ((BranchPair) obj).getSecondBranch().equals(this.secondBranch));
        }

        return false;
    }

    // Check if the second branch is dependent to the first one
    public boolean isDependent(){
        if(expression == null){
            return false;
        }
        return true;
    }


    public boolean getExpression(){
        if(!isDependent()){
            throw new IllegalArgumentException("The branches are not dependent");
        }

        return expression;
    }

    public BytecodeInstruction getCallSite() {
        return callSite;
    }
}
