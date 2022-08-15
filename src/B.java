import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class B {

    static final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    static final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out));

    static final String REGEX = "[A-Z][A-Z0-9']*|[|&()!]|->";

    static final List<String> tokens = new ArrayList<>();

    static final Pattern pattern = Pattern.compile(REGEX);

    static Deque<String> extendTokens;

    static int it;

    static final Set<Expression> context = new HashSet<>();

    static String token = "BEGIN";

    static final Map<String, Expression> match = new HashMap<>();

    static final List<Expression> previous = new ArrayList<>();
    static final Expression[] axioms = new Expression[]{
            parseString("A -> B -> A"),
            parseString("(A -> B) -> (A -> B -> C) -> (A -> C)"),
            parseString("A -> B -> A&B"),
            parseString("A&B -> A"),
            parseString("A&B -> B"),
            parseString("A -> A | B"),
            parseString("B -> A | B"),
            parseString("(A -> C) -> (B -> C) -> (A | B -> C)"),
            parseString("(A -> B) -> (A -> !B) -> !A"),
            parseString("!!A -> A")
    };

//    static final Map<String, Integer> values = new TreeMap<>();

    public static void main(String[] args) throws IOException {
        String line = reader.readLine();
        String[] parts = line.split(",|\\|-");
        IntStream.range(0, parts.length - 2).forEach(i -> context.add(parseString(parts[i])));
        Expression alpha = parseString(parts[parts.length - 2]), beta = parseString(parts[parts.length - 1]);
        for (int i = 0; i < parts.length - 2; i++) {
            if (i == 0) {
                writer.write(parts[i]);
            } else {
                writer.write(", " + parts[i]);
            }
        }
        writer.write(String.format("|- %s -> %s%n", alpha, beta));
        boolean hasNextLine = true;
        while (hasNextLine) {
            Expression deltaI = parseString(reader.readLine());
            if (deltaI.equals(beta))
                hasNextLine = false;
            if (deltaI.equals(alpha)) {
                writer.write(String.format("%1$s -> (%1$s -> %1$s)%n", alpha));
                writer.write(String.format("%1$s -> ((%1$s -> %1$s) -> %1$s)%n",
                        alpha));
                writer.write(String.format(
                        "(%1$s -> (%1$s -> %1$s)) -> ((%1$s -> ((%1$s -> %1$s) -> %1$s)) -> (%1$s -> %1$s))%n", alpha));
                writer.write(String.format("(%1$s -> ((%1$s -> %1$s) -> %1$s)) -> (%1$s -> %1$s)%n", alpha));
                writer.write(String.format("%1$s -> %1$s%n", alpha));
//                writer.write("equals\n");
            } else if (context.contains(deltaI)) {
                writer.write(deltaI + "\n");
                writer.write(String.format("%s -> (%s -> %s)%n", deltaI, alpha, deltaI));
                writer.write(String.format("%s -> %s%n", alpha, deltaI));
//                writer.write("context\n");
            } else if (isAxiom(deltaI)) {
                writer.write(deltaI + "\n");
                writer.write(String.format("%s -> (%s -> %s)%n", deltaI, alpha, deltaI));
                writer.write(String.format("%s -> %s%n", alpha, deltaI));
//                writer.write("axiom\n");
            } else {
                boolean found = false;
                label:
                for (Expression deltaK : previous) {
                    if ((deltaK instanceof Impl) && ((Impl) deltaK).b.equals(deltaI)) {
                        Expression deltaJ = ((Impl) deltaK).a;
                        for (Expression e : previous) {
                            if (e.equals(deltaJ)) {
                                writer.write(
                                        String.format(
                                                "(%1$s -> %2$s) -> ((%1$s -> (%2$s -> %3$s)) -> (%1$s -> %3$s))%n",
                                                alpha, deltaJ, deltaI));
                                writer.write(
                                        String.format("(%1$s -> (%2$s -> %3$s)) -> (%1$s -> %3$s)%n", alpha, deltaJ,
                                                deltaI));
                                writer.write(String.format("%s -> %s%n", alpha, deltaI));
//                                previous.add(new Impl(alpha, deltaI));
                                found = true;
                                break label;
                            }
                        }
                    }
                }
                if (!found) {
                    writer.write(deltaI + "\n");
                    writer.write(String.format("%s -> (%s -> %s)%n", deltaI, alpha, deltaI));
                    writer.write(String.format("%s -> %s%n", alpha, deltaI));
//                    writer.write("axiom\n");
                } /*else {
//                    writer.write("M.P.\n");
                }*/
            }
            previous.add(deltaI);
        }
        writer.close();
        reader.close();
    }

    static boolean isAxiom(Expression e) {
        for (Expression axiom : axioms) {
            match.clear();
            if (axiom.matchAxiom(e, match)) {
                return true;
            }
        }
        return false;
    }

    static Expression parseString(String expr) {
        tokens.clear();
        Matcher matcher = pattern.matcher(expr);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        it = tokens.size() - 1;
        extendTokens = implicationBrackets();
        return parseImpl();
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
//                values.putIfAbsent(token, null);
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

        boolean equals(Expression e);

        boolean matchAxiom(Expression e, Map<String, Expression> match);
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

        @Override
        public boolean equals(Expression e) {
            return (e instanceof Unary) && sign().equals(((Unary) e).sign()) && a.equals(((Unary) e).a);
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Expression) && equals((Expression) o);
        }

        @Override
        public String toString() {
            return String.format("%s%s", sign(), a);
        }

        abstract String sign();

        @Override
        public int hashCode() {
            return Objects.hash(a, sign());
        }

        @Override
        public boolean matchAxiom(Expression e, Map<String, Expression> match) {
            return (e instanceof Unary) && ((Unary) e).sign().equals(sign()) && a.matchAxiom(((Unary) e).a, match);
        }
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

        @Override
        public boolean equals(Expression e) {
            return (e instanceof Binary) && sign().equals(((Binary) e).sign()) && a.equals(((Binary) e).a) && b.equals(
                    ((Binary) e).b);
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Expression) && equals((Expression) o);
        }

        @Override
        public String toString() {
            return String.format("(%s %s %s)", a, sign(), b);
        }

        abstract String sign();

        @Override
        public int hashCode() {
            return Objects.hash(a, b, sign());
        }

        @Override
        public boolean matchAxiom(Expression e, Map<String, Expression> match) {
            return (e instanceof Binary) && ((Binary) e).sign().equals(sign()) &&
                    a.matchAxiom(((Binary) e).a, match) && b.matchAxiom(((Binary) e).b, match);
        }
    }

    static class Variable implements Expression {

        final String variable;

        Variable(String variable) {
            this.variable = variable;
        }

        @Override
        public int eval() {
            return 0;
        }

        @Override
        public boolean equals(Expression e) {
            return (e instanceof Variable) && ((Variable) e).variable.equals(variable);
        }

        @Override
        public boolean matchAxiom(Expression e, Map<String, Expression> match) {
            if (match.putIfAbsent(variable, e) == null) {
                return true;
            } else {
                return match.get(variable).equals(e);
            }
        }

        @Override
        public int hashCode() {
            return variable.hashCode();
        }

        @Override
        public String toString() {
            return variable;
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

        @Override
        String sign() {
            return "&";
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

        @Override
        String sign() {
            return "|";
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

        @Override
        String sign() {
            return "->";
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

        @Override
        String sign() {
            return "!";
        }
    }
}
