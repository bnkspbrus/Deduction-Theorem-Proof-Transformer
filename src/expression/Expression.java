package expression;

import java.util.Map;

public interface Expression {
    int eval(Map<String, Integer> values);

    boolean matchAxiom(Expression e, Map<String, Expression> match);
}
