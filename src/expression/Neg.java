package expression;

public class Neg extends Unary {

    Neg(Expression a) {
        super(a);
    }

    @Override
    int calc(int a) {
        return 1 - a;
    }

    @Override
    String sign() {
        return "!";
    }
}
