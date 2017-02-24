package patch;


public final class Patch {
    public final String toHide;
    public final String toReveal;
    public final String targetValue;

    public Patch(String toHide, String toReveal, String targetValue) {
        this.toHide = toHide;
        this.toReveal = toReveal;
        this.targetValue = targetValue;
    }
}
