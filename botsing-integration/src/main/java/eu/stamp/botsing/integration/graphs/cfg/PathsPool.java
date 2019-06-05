package eu.stamp.botsing.integration.graphs.cfg;

import org.evosuite.graphs.cfg.BasicBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PathsPool {

    private static PathsPool instance;

    // maps for call integration points

    // map for independent paths in a method : <ClassName,<MethodName, List<Paths>>>
    private Map<String,Map<String,List<List<BasicBlock>>>> pathsForMethod = new HashMap<>();
    // map of independent paths from entry point to a call_site (BasicBlock): <ClassName,<MethodName,<call_site(BasicBock), List<Paths>>>>
    private Map<String,Map<String,Map<BasicBlock,List<List<BasicBlock>>>>> pathsToCallSites = new HashMap<>();

    // maps for return integration points
    // information related to paths from entry point to return should be determinable from pathForMethod.
    // map for after call paths from a call_site to one of the exit points: <ClassName,<MethodName,<call_site(BasicBock), List<Paths>>>>
    private Map<String,Map<String,Map<BasicBlock,List<List<BasicBlock>>>>> pathFromCallSites = new HashMap<>();

    private PathsPool(){
    }


    public static PathsPool getInstance(){
        if(instance == null){
            instance = new PathsPool();
        }

        return instance;
    }

    public void registerNewPathsForMethod(String className, String methodName, List<List<BasicBlock>> paths){
        if(!pathsForMethod.containsKey(className)){
            pathsForMethod.put(className,new HashMap<>());
        }

        if(!pathsForMethod.get(className).containsKey(methodName)){
            pathsForMethod.get(className).put(methodName,new ArrayList<>());
        }

        pathsForMethod.get(className).get(methodName).addAll(paths);
    }


    public void registerNewPathsForCallSite(String className, String methodName, BasicBlock callSite, List<List<BasicBlock>> paths){
        if(!pathsToCallSites.containsKey(className)){
            pathsToCallSites.put(className,new HashMap<>());
        }

        if(!pathsToCallSites.get(className).containsKey(methodName)){
            pathsToCallSites.get(className).put(methodName, new HashMap<>());
        }

        if(!pathsToCallSites.get(className).get(methodName).containsKey(callSite)){
            pathsToCallSites.get(className).get(methodName).put(callSite, new ArrayList<>());
        }

        pathsToCallSites.get(className).get(methodName).get(callSite).addAll(paths);
    }
}
