package expression;

public class Or extends Binary {

    protected Or(Expression a, Expression b) {
        super(a, b);
    }

    @Override
    int calc(int a, int b) {
        return a | b;
    }

    @Override
    String sign() {
        return "|";
    }
}
