package patch;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;


/**
 * Created by justusadam on 24/02/2017.
 */
class GetModifyingClassVisitor extends ClassVisitor {
    private static final Type STRING_TYPE = Type.getType(String.class);
    static final Method[] METHODS = new Method[]{
            new Method("get", STRING_TYPE, new Type[]{STRING_TYPE}),
            new Method("get", STRING_TYPE, new Type[]{STRING_TYPE, STRING_TYPE})
    };
    private static final Method EQUALS_METHOD = new Method("equals", Type.BOOLEAN_TYPE, new Type[]{Type.getType(Object.class)});
    private final Iterable<Patch> rewrites;

    GetModifyingClassVisitor(final int api, final ClassVisitor cv, final Iterable<Patch> rewrites) {
        super(api, cv);
        this.rewrites = rewrites;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        final MethodVisitor fromSuper = super.visitMethod(access, name, desc, signature, exceptions);
        for (Method m :
                METHODS) {
            if (name.equals(m.getName()) && desc.equals(m.getDescriptor()))
                return new GetModifyingMethodVisitor(fromSuper, m);
        }
        return fromSuper;
    }

    private final class GetModifyingMethodVisitor extends GeneratorAdapter {

        GetModifyingMethodVisitor(final MethodVisitor mv, final Method method) {
            super(GetModifyingClassVisitor.this.api, mv, ACC_PUBLIC, method.getName(), method.getDescriptor());
        }

        @Override
        public void visitCode() {
            for (Patch rewrite :
                    rewrites) {

                Label l1 = newLabel();

                push(rewrite.toHide);
                loadArg(0);
                invokeVirtual(Type.getType(String.class), EQUALS_METHOD);
                ifZCmp(GeneratorAdapter.EQ, l1);
                push(rewrite.targetValue);
                returnValue();

                visitLabel(l1);

                Label l2 = newLabel();

                loadArg(0);
                push(rewrite.toReveal);
                invokeVirtual(Type.getType(String.class), EQUALS_METHOD);
                ifZCmp(GeneratorAdapter.EQ, l2);
                push(rewrite.toHide);
                storeArg(0);

                visitLabel(l2);
            }

            super.visitCode();
        }
    }
}
