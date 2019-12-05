package eu.stamp.cbc.instrument;

import org.evosuite.classpath.ResourceList;
import org.evosuite.instrumentation.LineNumberMethodAdapter;
import org.evosuite.runtime.classhandling.ClassResetter;
import org.evosuite.setup.TestClusterUtils;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class CBClassAdapter extends ClassVisitor {

    protected final String className;

    /** Skip methods on enums - at least some */
    protected boolean isEnum = false;

    /** Skip default constructors on anonymous classes */
    protected boolean isAnonymous = false;

    public CBClassAdapter(ClassVisitor visitor, String className) {
        super(327680, visitor);
        this.className = ResourceList.getClassNameFromResourcePath(className);
    }


    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        if (superName.equals("java/lang/Enum")){
            isEnum = true;
        }

        if(TestClusterUtils.isAnonymousClass(name)){
            isAnonymous = true;
        }
    }


    @Override
    public MethodVisitor visitMethod(int methodAccess, String name, String descriptor,
                                     String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(methodAccess, name, descriptor, signature,
                exceptions);


        if ((methodAccess & Opcodes.ACC_SYNTHETIC) > 0
                || (methodAccess & Opcodes.ACC_BRIDGE) > 0) {
            return mv;
        }

        if (name.equals("<clinit>")){
            return mv;
        }

        if (name.equals(ClassResetter.STATIC_RESET)){
            return mv;
        }

        if(className.contains("_ESTest")){
            return mv;
        }

        if (isEnum && (name.equals("valueOf") || name.equals("values"))) {
            return mv;
        }

        mv = new LineNumberMethodAdapter(mv, className, name, descriptor);

        return mv;

    }
}
