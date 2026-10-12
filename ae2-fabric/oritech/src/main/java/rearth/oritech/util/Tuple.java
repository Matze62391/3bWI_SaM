package rearth.oritech.util;

/** A mutable pair (Minecraft's own Tuple was removed in 26.3). */
public class Tuple<A, B> {
    private A a;
    private B b;

    public Tuple(A a, B b) {
        this.a = a;
        this.b = b;
    }

    public A getA() {
        return a;
    }

    public void setA(A a) {
        this.a = a;
    }

    public B getB() {
        return b;
    }

    public void setB(B b) {
        this.b = b;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Tuple<?, ?> other && java.util.Objects.equals(a, other.a) && java.util.Objects.equals(b, other.b);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(a, b);
    }
}
