package eu.stamp.botsing.integration.graphs.cfg;

import org.evosuite.graphs.cfg.BasicBlock;
import org.evosuite.graphs.cfg.BytecodeInstruction;

import java.util.*;

public class PathsPool {

    private static PathsPool instance;

    // maps for call integration points

    // map for independent paths in a method : <ClassName,<MethodName, List<Paths>>>
    private Map<String,Map<String,List<List<BasicBlock>>>> pathsForMethod = new HashMap<>();
    // map of independent paths from entry point to a call_site (BasicBlock): <ClassName,<MethodName,<call_site(bytecode), List<Paths>>>>
    private Map<String,Map<String,Map<BytecodeInstruction,List<List<BasicBlock>>>>> pathsToCallSites = new HashMap<>();

    // maps for return integration points
    // information related to paths from entry point to return should be determinable from pathForMethod.
    // map for after call paths from a call_site to one of the exit points: <ClassName,<MethodName,<call_site(bytecode), List<Paths>>>>
    private Map<String,Map<String,Map<BytecodeInstruction,List<List<BasicBlock>>>>> pathFromCallSites = new HashMap<>();

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


    public void registerNewPathsToCallSite(String className, String methodName, BytecodeInstruction callSite, List<List<BasicBlock>> paths){
        if(!pathsToCallSites.containsKey(className)){
            pathsToCallSites.put(className,new HashMap<>());
        }

        if(!pathsToCallSites.get(className).containsKey(methodName)){
            pathsToCallSites.get(className).put(methodName, new HashMap<>());
        }

        if(!pathsToCallSites.get(className).get(methodName).containsKey(callSite)){
            pathsToCallSites.get(className).get(methodName).put(callSite, new ArrayList<>());
        }

        for (List<BasicBlock> path: paths){
            if(pathsToCallSites.get(className).get(methodName).get(callSite).contains(path)){
                continue;
            }
            pathsToCallSites.get(className).get(methodName).get(callSite).add(path);
        }


    }


    public void registerNewPathsFromCallSite(String className, String methodName, BytecodeInstruction callSite, List<List<BasicBlock>> paths){
        if(!pathFromCallSites.containsKey(className)){
            pathFromCallSites.put(className,new HashMap<>());
        }

        if(!pathFromCallSites.get(className).containsKey(methodName)){
            pathFromCallSites.get(className).put(methodName, new HashMap<>());
        }

        if(!pathFromCallSites.get(className).get(methodName).containsKey(callSite)){
            pathFromCallSites.get(className).get(methodName).put(callSite, new ArrayList<>());
        }

        for (List<BasicBlock> path: paths){
            if(pathFromCallSites.get(className).get(methodName).get(callSite).contains(path)){
                continue;
            }
            pathFromCallSites.get(className).get(methodName).get(callSite).add(path);
        }
    }


    public List<List<BasicBlock>[]> getPathPairs(String className){
        List<List<BasicBlock>[]> pathPairs = new ArrayList<>();
        for (BytecodeInstruction callsite: getCallSites(className)){
            List<List<BasicBlock>> pathsToCallSite = pathsToCallSites.get(className).get(callsite.getMethodName()).get(callsite);
            List<List<BasicBlock>> pathsFromCallSite = pathFromCallSites.get(className).get(callsite.getMethodName()).get(callsite);
            String calledClass = callsite.getCalledMethodsClass();
            String calledMethod = callsite.getCalledMethod();

            for (List<BasicBlock> path1: pathsToCallSite){
                for (List<BasicBlock> path2: pathsForMethod.get(calledClass).get(calledMethod)){
                    pathPairs.add(createPair(path1,path2));
                }
            }


            for (List<BasicBlock> path1: pathsForMethod.get(calledClass).get(calledMethod)){
                for (List<BasicBlock> path2: pathsFromCallSite){
                    pathPairs.add(createPair(path1,path2));
                }
            }


        }
        return pathPairs;
    }

    private List<BasicBlock>[] createPair(List<BasicBlock> path1, List<BasicBlock> path2) {
        List<BasicBlock>[] pair = new List[2];
        pair[0]=path1;
        pair[1]=path2;

        return pair;
    }

    public Set<BytecodeInstruction> getCallSites(String className){
        if(!pathsToCallSites.containsKey(className)){
            throw new IllegalArgumentException("call_sites of class "+className+" are not available!");
        }
        Set<BytecodeInstruction> callSites = new HashSet<>();
        for (String methodName: pathsToCallSites.get(className).keySet()){
            callSites.addAll(getCallSites(className,methodName));
        }

        return callSites;
    }

    public Set<BytecodeInstruction> getCallSites(String className, String methodName) {
        if(!pathsToCallSites.containsKey(className) || !pathsToCallSites.get(className).containsKey(methodName)){
            throw new IllegalArgumentException("call_sites of class "+className+" and method "+methodName+" are not available!");
        }
        return pathsToCallSites.get(className).get(methodName).keySet();
    }



}
