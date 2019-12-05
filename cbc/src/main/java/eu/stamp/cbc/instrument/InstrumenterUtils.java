package eu.stamp.cbc.instrument;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InnerClassNode;

public class InstrumenterUtils {
    public static boolean isInterface(ClassNode classNode){
        return (classNode.access & Opcodes.ACC_INTERFACE) != 0 ;
    }

    public static boolean isPrivate(ClassNode classNode){
        return (classNode.access & Opcodes.ACC_PRIVATE) != 0 ;
    }

    public static boolean isPrivate(InnerClassNode innerClassNode){
        return (innerClassNode.access & Opcodes.ACC_PRIVATE) != 0 ;
    }


    public static String getPackageName(String internalClassName){
        String packageName = internalClassName.replace('/', '.');

        if(packageName.contains(".")){
            packageName = packageName.substring(0, packageName.lastIndexOf('.'));
        }
        return packageName;
    }


    public static InnerClassNode checkIfInnerClass(ClassNode classNode){
        for(InnerClassNode innerClass: classNode.innerClasses){
            if (classNode.name.equals(innerClass.name)){
                return innerClass;
            }
        }
        return null;
    }
}
