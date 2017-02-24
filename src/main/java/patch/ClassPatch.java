package patch;

/**
 * Created by justusadam on 24/02/2017.
 */
public final class ClassPatch {
    public final Class targetClass;
    public final Patch[] patchesToApply;

    public ClassPatch(final Class targetClass, final Patch[] patchesToApply) {
        this.targetClass = targetClass;
        this.patchesToApply = patchesToApply;
    }
}
