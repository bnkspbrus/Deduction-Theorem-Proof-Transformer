import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class A {

    static final Map<String, Integer> values = new TreeMap<>();

    static final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out));

    static final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    static Matcher matcher;

    static final String REGEX = "[A-Z][A-Z0-9']*|[|&()!]|->";

    static final Pattern pattern = Pattern.compile(REGEX);

    static String token = "BEGIN";

    static final List<String> tokens = new ArrayList<>();
    static Deque<String> extendTokens;

    static int it; // iterator

    public static void main(String[] args) throws IOException {
        String expr = reader.readLine();
        matcher = pattern.matcher(expr);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        it = tokens.size() - 1;
        extendTokens = implicationBrackets();
        Expression parsed = parseImpl();
        int counter = 0;
        for (int i = 0; i < 1 << values.size(); i++) {
            int j = 0;
            for (Map.Entry<String, Integer> entry : values.entrySet()) {
                entry.setValue((i & (1 << j++)) > 0 ? 1 : 0);
            }
            counter += parsed.eval();
        }
        if (counter == (1 << values.size())) {
            writer.write("Valid\n");
        } else if (counter == 0) {
            writer.write("Unsatisfiable\n");
        } else {
            writer.write(String.format("Satisfiable and invalid, %d true and %d false cases%n", counter,
                    (1 << values.size()) - counter));
        }
        writer.close();
        reader.close();
    }

    static Expression parseImpl() {
        Expression left = parseOr();
        while (token.equals("->")) {
            left = new Impl(left, parseOr());
        }
        return left;
    }

    static Expression parseOr() {
        Expression left = parseAnd();
        while (token.equals("|")) {
            left = new Or(left, parseAnd());
        }
        return left;
    }

    static Expression parseAnd() {
        Expression left = parseFirstPriority();
        while (token.equals("&")) {
            left = new And(left, parseFirstPriority());
        }
        return left;
    }

    static Expression parseFirstPriority() {
        Expression res;
        switch (nextToken()) {
            case "!":
                res = new Neg(parseFirstPriority());
                break;
            case "(":
                res = parseImpl();
                nextToken();
                break;
            default:
                values.putIfAbsent(token, null);
                res = new Variable(token);
                nextToken();
        }
        return res;
    }

    static Deque<String> implicationBrackets() {
        Deque<String> expr = new ArrayDeque<>();
        label:
        while (it >= 0) {
            expr.addLast(tokens.get(it--));
            switch (tokens.get(it + 1)) {
                case "->":
                    String impl = expr.removeLast();
                    expr.addLast("(");
                    expr.addLast(impl);
                    expr.addFirst(")");
                    break;
                case ")":
                    expr.addAll(implicationBrackets());
                    break;
                case "(":
                    break label;
            }
        }
        return expr;
    }

    static String nextToken() {
        if (!extendTokens.isEmpty()) {
            return token = extendTokens.pollLast();
        } else {
            return token = "END";
        }
    }

    interface Expression {
        int eval();
    }

    static abstract class Unary implements Expression {
        final Expression a;

        protected Unary(Expression a) {
            this.a = a;
        }

        @Override
        public int eval() {
            return calc(a.eval());
        }

        abstract int calc(int a);
    }

    static abstract class Binary implements Expression {
        final Expression a, b;

        protected Binary(Expression a, Expression b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public int eval() {
            return calc(a.eval(), b.eval());
        }

        abstract int calc(int a, int b);
    }

    static class Variable implements Expression {

        final String variable;

        Variable(String variable) {
            this.variable = variable;
        }

        @Override
        public int eval() {
            return values.get(variable);
        }
    }

    static class And extends Binary {

        protected And(Expression a, Expression b) {
            super(a, b);
        }

        @Override
        int calc(int a, int b) {
            return a & b;
        }
    }

    static class Or extends Binary {

        protected Or(Expression a, Expression b) {
            super(a, b);
        }

        @Override
        int calc(int a, int b) {
            return a | b;
        }
    }

    static class Impl extends Binary {

        protected Impl(Expression a, Expression b) {
            super(a, b);
        }

        @Override
        int calc(int a, int b) {
            return (1 - a) | b;
        }
    }

    static class Neg extends Unary {

        Neg(Expression a) {
            super(a);
        }

        @Override
        int calc(int a) {
            return 1 - a;
        }
    }
}
