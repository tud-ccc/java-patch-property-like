package patch;

/**
 * Created by justusadam on 24/02/2017.
 */
public final class ClassPatch {
    public final Class targetClass;
    public final Iterable<Patch> patchesToApply;

    public ClassPatch(Class targetClass, Iterable<Patch> patchesToApply) {
        this.targetClass = targetClass;
        this.patchesToApply = patchesToApply;
    }
}
