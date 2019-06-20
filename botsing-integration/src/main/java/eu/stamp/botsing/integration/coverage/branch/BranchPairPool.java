package eu.stamp.botsing.integration.coverage.branch;

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


    public void addPair(Branch firstBranch, Branch secondBranch,  BytecodeInstruction callSite){
        BranchPair branchPair = new BranchPair(firstBranch,secondBranch, callSite);
        pool.add(branchPair);
    }

    public void addPair(Branch firstBranch, Branch secondBranch,  BytecodeInstruction callSite, boolean expression){
        BranchPair branchPair = new BranchPair(firstBranch,secondBranch, callSite,expression);
        pool.add(branchPair);
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
