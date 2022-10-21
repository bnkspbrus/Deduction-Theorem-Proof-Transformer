package expression;

import java.util.Map;
import java.util.Objects;

public abstract class Unary implements Expression {
    final Expression expr;

    protected Unary(Expression expr) {
        this.expr = expr;
    }

    @Override
    public int eval(Map<String, Integer> values) {
        return calc(expr.eval(values));
    }

    abstract int calc(int a);

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Unary unary = (Unary) o;
        return Objects.equals(sign(), unary.sign()) && Objects.equals(expr, unary.expr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sign(), expr);
    }

    @Override
    public String toString() {
        return String.format("%s%s", sign(), expr);
    }

    abstract String sign();

    @Override
    public boolean matchAxiom(Expression unary, Map<String, Expression> match) {
        return (unary instanceof Unary) && ((Unary) unary).sign().equals(sign()) && expr.matchAxiom(
                ((Unary) unary).expr, match);
    }
}
