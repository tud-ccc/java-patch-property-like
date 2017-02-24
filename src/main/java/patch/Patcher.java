package patch;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.stream.StreamSupport;

public final class Patcher {
    private final int api;

    public Patcher(int api) {
        this.api = api;
    }

    public byte[] patchByteCode(InputStream originalByteCode, Iterable<Patch> patches) throws IOException {
        final ClassReader cr = new ClassReader(originalByteCode);

        final ClassWriter cw = new ClassWriter(cr,ClassWriter.COMPUTE_MAXS + ClassWriter.COMPUTE_FRAMES);

        final ClassVisitor cv = new GetModifyingClassVisitor(api, cw, patches);

        cr.accept(cv, ClassReader.EXPAND_FRAMES);

        return cw.toByteArray();
    }

    public byte[] patchClass(final ClassPatch classPatch) throws IOException {
        return patchClass(classPatch.targetClass, classPatch.patchesToApply);
    }

    public byte[] patchClass(final Class clz, final Iterable<Patch> patches) throws IOException {

        final InputStream is = clz.getClassLoader().getResourceAsStream(
                clz.getCanonicalName().replace(".", "/") + ".class");
        return patchByteCode(is, patches);
    }

    public void patchClassesAndRedefine(final Instrumentation instrumentation, final Iterable<ClassPatch> classPatches) throws IOException, ClassNotFoundException, UnmodifiableClassException {
        instrumentation.redefineClasses(
                StreamSupport.stream(classPatches.spliterator(),false).map(cp -> {
                    try {
                        return new ClassDefinition(cp.targetClass, patchClass(cp));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).toArray(ClassDefinition[]::new)
        );
    }
}
