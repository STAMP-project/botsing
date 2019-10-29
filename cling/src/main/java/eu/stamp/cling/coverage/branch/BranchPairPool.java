package eu.stamp.cling.coverage.branch;

import org.evosuite.coverage.branch.Branch;
import org.evosuite.graphs.cfg.BytecodeInstruction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BranchPairPool {
    private static BranchPairPool instance;
    // pool of branch pairs
    List<BranchPair> pool = new ArrayList<>();

    private BranchPairPool(){}

    public static BranchPairPool getInstance(){
        if(instance == null){
            instance = new BranchPairPool();
        }

        return instance;
    }

    private void addPair(BranchPair branchPair){
        if (pool.contains(branchPair)){
            return;
        }
        pool.add(branchPair);
    }

    public void addPair(Branch firstBranch, Branch secondBranch,  BytecodeInstruction callSite){
        BranchPair branchPair = new BranchPair(firstBranch,secondBranch, callSite);
        addPair(branchPair);
    }

    public void addPair(Branch firstBranch, Branch secondBranch,  BytecodeInstruction callSite, boolean expression){
        BranchPair branchPair = new BranchPair(firstBranch,secondBranch, callSite,expression);
        addPair(branchPair);
    }

    public Set<String> getSetOfMethodsWithCallSite(){
        Set<String> result = new HashSet<>();
        for(BranchPair pair: pool){
            if(!result.contains(pair.getCallSite().getMethodName())){
                result.add(pair.getCallSite().getMethodName());
            }

        }
        return result;
    }

    public List<BranchPair> getBranchPairs() {
        return pool;
    }
}
