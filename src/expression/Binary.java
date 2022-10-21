package expression;

import java.util.Map;
import java.util.Objects;

public abstract class Binary implements Expression {
    public final Expression left;
    public final Expression right;

    protected Binary(Expression left, Expression right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Binary binary = (Binary) o;
        return Objects.equals(sign(), binary.sign())
                && Objects.equals(left, binary.left)
                && Objects.equals(right, binary.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right, sign());
    }

    @Override
    public int eval(Map<String, Integer> values) {
        return calc(left.eval(values), right.eval(values));
    }

    abstract int calc(int a, int b);

    @Override
    public String toString() {
        return String.format("(%s %s %s)", left, sign(), right);
    }

    abstract String sign();

    @Override
    public boolean matchAxiom(Expression binary, Map<String, Expression> match) {
        return (binary instanceof Binary) && ((Binary) binary).sign().equals(sign()) &&
                left.matchAxiom(((Binary) binary).left, match) && right.matchAxiom(((Binary) binary).right, match);
    }
}
