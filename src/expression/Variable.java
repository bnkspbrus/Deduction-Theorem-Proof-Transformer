package expression;

import java.util.Map;
import java.util.Objects;

public class Variable implements Expression {
    final String variable;

    final Map<String, Integer> values;

    Variable(String variable) {
        this.variable = variable;
        this.values = null;
    }

    @Override
    public int eval(Map<String, Integer> values) {
        return values.get(variable);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Variable variable1 = (Variable) o;
        return Objects.equals(variable, variable1.variable);
    }

    @Override
    public boolean matchAxiom(Expression var, Map<String, Expression> match) {
        if (match.putIfAbsent(variable, var) == null) {
            return true;
        } else {
            return match.get(variable).equals(var);
        }
    }

    @Override
    public String toString() {
        return variable;
    }

    @Override
    public int hashCode() {
        return variable.hashCode();
    }
}
