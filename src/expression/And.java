package expression;

public class And extends Binary {

    protected And(Expression a, Expression b) {
        super(a, b);
    }

    @Override
    int calc(int a, int b) {
        return a & b;
    }

    @Override
    String sign() {
        return "&";
    }
}