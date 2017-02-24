package patch;

import jdk.nashorn.internal.objects.ArrayBufferView;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.Method;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class Patcher {
    private final int api;

    public Patcher(final int api) {
        this.api = api;
    }

    public byte[] patchByteCode(final InputStream originalByteCode, final Patch... patches) throws IOException {

        final ClassReader cr = new ClassReader(originalByteCode);

        final ClassWriter cw = new ClassWriter(cr,ClassWriter.COMPUTE_MAXS + ClassWriter.COMPUTE_FRAMES);

        final ClassVisitor cv = new GetModifyingClassVisitor(api, cw, patches);

        cr.accept(cv, ClassReader.EXPAND_FRAMES);

        return cw.toByteArray();
    }

    public byte[] patchClass(final ClassPatch classPatch) throws IOException {
        return patchClass(classPatch.targetClass, classPatch.patchesToApply);
    }

    public byte[] patchClass(final Class clz, final Patch... patches) throws IOException {

        final InputStream is = clz.getClassLoader().getResourceAsStream(
                clz.getCanonicalName().replace(".", "/") + ".class");
        return patchByteCode(is, patches);
    }

    private static boolean hasMethod(Class clz, Method m) {
        try {
            clz.getDeclaredMethod(m.getName(), Arrays.stream(m.getArgumentTypes()).map(t -> {
                try {
                    return Class.forName(t.getClassName());
                } catch (ClassNotFoundException c) {
                    throw new RuntimeException("This should never happen", c);
                }
            }).toArray(Class[]::new));
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static void throwOnlyOneMethodFound() throws NoApplicableClassException {
        throw new NoApplicableClassException("Found class which implements one 'get' method but not the other.");
    }

    public static Class findApplicableClass(Class clz) throws NoApplicableClassException {
        while (true) {

            if (hasMethod(clz, GetModifyingClassVisitor.METHODS[0])) {
                if (hasMethod(clz, GetModifyingClassVisitor.METHODS[1])) return clz;
                else throwOnlyOneMethodFound();
            }
            else if (hasMethod(clz, GetModifyingClassVisitor.METHODS[1])) throwOnlyOneMethodFound();
            else if (clz.equals(Object.class)) throw new NoApplicableClassException("Reached top of inheritance hierarchy.");
        }
    }

    public byte[] findAndPatchApplicableClass(final Class rootClass, final Patch... patches) throws IOException, NoApplicableClassException {
        return patchClass(findApplicableClass(rootClass), patches);
    }

    public byte[] findAndPatchApplicableClass(final ClassPatch classPatch) throws IOException, NoApplicableClassException {
        return findAndPatchApplicableClass(classPatch.targetClass, classPatch.patchesToApply);
    }

    public void patchClassesAndRedefine(final Instrumentation instrumentation, final ClassPatch... classPatches) throws IOException, ClassNotFoundException, UnmodifiableClassException {
        instrumentation.redefineClasses(
                Arrays.stream(classPatches).map(cp -> {
                    try {
                        return new ClassDefinition(cp.targetClass, patchClass(cp));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).toArray(ClassDefinition[]::new)
        );
    }

    public void findPatchAndRedefineClasses(final Instrumentation instrumentation, final ClassPatch[] classPatches) throws IOException, ClassNotFoundException, UnmodifiableClassException, NoApplicableClassException {
        patchClassesAndRedefine(
                instrumentation,
                Arrays.stream(classPatches).map(cp -> {
                    try {
                        return new ClassPatch(findApplicableClass(cp.targetClass), cp.patchesToApply);
                    } catch (NoApplicableClassException e) {
                        throw new RuntimeException(e);
                    }
                }).toArray(ClassPatch[]::new)
        );
    }
}
