package eu.stamp.cbc.instrument;

import org.evosuite.Properties;
import org.evosuite.instrumentation.ExecutionPathClassAdapter;
import org.evosuite.instrumentation.LineNumberMethodAdapter;
import org.evosuite.testcarver.instrument.TransformerUtil;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.TraceClassVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Instrumenter {
    private static final Logger LOG = LoggerFactory.getLogger(Instrumenter.class);

    public void transformClassNode(ClassNode classNode, final String internalClassName, ClassWriter writer)
    {

        // Check EvoSuite blacklist
        if(! TransformerUtil.isClassConsideredForInstrumentation(internalClassName))
        {
            LOG.debug("Class {} has not been instrumented because its name is on the blacklist", internalClassName);
            return;
        }

        // Check if the given class is interface
        if(InstrumenterUtils.isInterface(classNode)) {
            LOG.debug("Class {} is an interface.",classNode.name);
            return;
        }

        // Check if the given class is private
        if(InstrumenterUtils.isPrivate(classNode)){
            LOG.debug("Class {} is private.",classNode.name);
            return;
        }

        String packageName = InstrumenterUtils.getPackageName(internalClassName);



        // Check if the given class is an inner class
        InnerClassNode innerClass = InstrumenterUtils.checkIfInnerClass(classNode);
        if(innerClass != null){
            LOG.debug("InnerClass equals class. Check if it is accessible");
            if (InstrumenterUtils.isPrivate(innerClass)){
                return;
            }
        }

//        logger.info("Checking package {} for class {}", packageName, cn.name);

//        // Protected/default only if in same package
//        if((cn.access & Opcodes.ACC_PUBLIC) == 0) {
//            if(!Properties.CLASS_PREFIX.equals(packageName)) {
//                logger.info("Not using protected/default class because package name does not match");
//                return;
//            } else {
//                logger.info("Using protected/default class because package name matches");
//            }
//        }
		/*
		if(	(cn.access & Opcodes.ACC_PUBLIC) == 0 && (cn.access & Opcodes.ACC_PROTECTED) == 0)
		{
			return;
		}
		*/

        ClassVisitor cv = writer;
        cv = new ExecutionPathClassAdapter(cv, classNode.name);
        // Check the methods
//        for(MethodNode methodNode:classNode.methods){
//            methodNode.accept(new LineNumberMethodAdapter(writer,classNode.name,methodNode.name,methodNode.desc));
//        }
//
//
//        final ArrayList<MethodNode> wrappedMethods = new ArrayList<MethodNode>();
//        MethodNode methodNode;
//
//        final Iterator<MethodNode> methodIter = cn.methods.iterator();
//        while(methodIter.hasNext())
//        {
//            methodNode = methodIter.next();
//
//            // consider only public methods which are not abstract or native
//            if( ! TransformerUtil.isPrivate(methodNode.access)  &&
//                    ! TransformerUtil.isAbstract(methodNode.access) &&
//                    ! TransformerUtil.isNative(methodNode.access)   &&
//                    ! methodNode.name.equals("<clinit>"))
//            {
//                if(! TransformerUtil.isPublic(methodNode.access)) {
//                    //if(!Properties.CLASS_PREFIX.equals(packageName)) {
//                    transformWrapperCalls(methodNode);
//                    continue;
//                    //}
//                }
//                if(methodNode.name.equals("<init>"))
//                {
//                    if(TransformerUtil.isAbstract(cn.access)) {
//                        // We cannot invoke constructors of abstract classes directly
//                        continue;
//                    }
//                    this.addFieldRegistryRegisterCall(methodNode);
//                }
//
//                this.instrumentPUTXXXFieldAccesses(cn, internalClassName, methodNode);
//                this.instrumentGETXXXFieldAccesses(cn, internalClassName, methodNode);
//
//                this.instrumentMethod(cn, internalClassName, methodNode, wrappedMethods);
//            } else {
//                transformWrapperCalls(methodNode);
//            }
//        }
//
//        final int numWM = wrappedMethods.size();
//        for(int i = 0; i < numWM; i++)
//        {
//            cn.methods.add(wrappedMethods.get(i));
//        }
//
//        TraceClassVisitor tcv = new TraceClassVisitor(new PrintWriter(System.err));
//        cn.accept(tcv);
    }
}
