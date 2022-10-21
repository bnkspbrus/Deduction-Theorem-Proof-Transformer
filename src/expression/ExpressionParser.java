package expression;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExpressionParser {
    static final String REGEX = "[A-Z][A-Z0-9']*|[|&()!]|->";
    static final Pattern pattern = Pattern.compile(REGEX);
    List<String> tokens;
    String token;
    int tokenIdx;

    public static void main(String[] args) {
        Map<String, Integer> variables = new TreeMap<>();
        ExpressionParser parser = new ExpressionParser();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out))) {
            Expression parsed = parser.parseString(reader.readLine(), variables);
            int counter = 0;
            for (int i = 0; i < 1 << variables.size(); i++) {
                int j = 0;
                for (Map.Entry<String, Integer> entry : variables.entrySet()) {
                    entry.setValue((i & (1 << j++)) > 0 ? 1 : 0);
                }
                counter += parsed.eval(variables);
            }
            if (counter == (1 << variables.size())) {
                writer.write("Valid\n");
            } else if (counter == 0) {
                writer.write("Unsatisfiable\n");
            } else {
                writer.write(String.format("Satisfiable and invalid, %d true and %d false cases%n", counter,
                        (1 << variables.size()) - counter));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Expression parseString(String expr) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = pattern.matcher(expr);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        this.tokens = new ArrayList<>(packImplIntoBrackets(tokens));
        Collections.reverse(this.tokens);
        tokenIdx = 0;
        return parseImpl();
    }

    public Expression parseString(String expr, Map<String, Integer> variables) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = pattern.matcher(expr);
        while (matcher.find()) {
            tokens.add(matcher.group());
            if (tokens.get(tokens.size() - 1).matches("[A-Z][A-Z0-9']*")) {
                variables.put(tokens.get(tokens.size() - 1), 0);
            }
        }
        this.tokens = new ArrayList<>(packImplIntoBrackets(tokens));
        Collections.reverse(this.tokens);
        tokenIdx = 0;
        return parseImpl();
    }

    Expression parseImpl() {
        Expression left = parseOr();
        while (token.equals("->")) {
            left = new Impl(left, parseOr());
        }
        return left;
    }

    Expression parseOr() {
        Expression left = parseAnd();
        while (token.equals("|")) {
            left = new Or(left, parseAnd());
        }
        return left;
    }

    Expression parseAnd() {
        Expression left = parseFirstPriority();
        while (token.equals("&")) {
            left = new And(left, parseFirstPriority());
        }
        return left;
    }

    Expression parseFirstPriority() {
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
                res = new Variable(token);
                nextToken();
                break;
        }
        return res;
    }

    Deque<String> packImplIntoBrackets(List<String> tokens) {
        Deque<String> deque = new ArrayDeque<>();
        label:
        while (!tokens.isEmpty()) {
            String token = tokens.remove(tokens.size() - 1);
            switch (token) {
                case "->":
                    deque.addLast("(");
                    deque.addLast(token);
                    deque.addFirst(")");
                    break;
                case ")":
                    deque.addLast(token);
                    deque.addAll(packImplIntoBrackets(tokens));
                    break;
                case "(":
                    deque.addLast(token);
                    break label;
                default:
                    deque.addLast(token);
            }
        }
        return deque;
    }

    String nextToken() {
        if (tokenIdx < tokens.size()) {
            return token = tokens.get(tokenIdx++);
        } else {
            return token = "END";
        }
    }
}
