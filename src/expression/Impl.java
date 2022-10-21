package expression;

public class Impl extends Binary {

    protected Impl(Expression a, Expression b) {
        super(a, b);
    }

    @Override
    int calc(int a, int b) {
        return (1 - a) | b;
    }

    @Override
    String sign() {
        return "->";
    }
}