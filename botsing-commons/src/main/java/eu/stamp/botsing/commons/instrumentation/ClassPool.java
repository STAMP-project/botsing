package eu.stamp.botsing.commons.instrumentation;

import java.util.ArrayList;
import java.util.List;

public class ClassPool {

    private static ClassPool instance;
    // pool of branch pairs
    List<Class> pool = new ArrayList<>();

    private ClassPool(){}

    public static ClassPool getInstance(){
        if(instance == null){
            instance = new ClassPool();
        }

        return instance;
    }

    public void registerClass(Class cls){
        if(!pool.contains(cls)){
            pool.add(cls);
        }
    }

    public Class fetchClass(String className){
        for (Class cls : this.pool){
            if(cls.getName().equals(className)){}
            return cls;
        }
        return null;
    }
}
