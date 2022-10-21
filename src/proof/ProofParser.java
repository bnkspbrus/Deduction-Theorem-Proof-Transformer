package proof;

import expression.Expression;
import expression.ExpressionParser;
import expression.Impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.stream.Collectors;

public class ProofParser {
    static final Expression[] AXIOMS;

    static {
        ExpressionParser expressionParser = new ExpressionParser();
        AXIOMS = new Expression[]{
                expressionParser.parseString("A -> B -> A"),
//                expressionParser.parseString("(A -> B -> C) -> (A -> B) -> (A -> C)"),
                expressionParser.parseString("(A -> B) -> (A -> B -> C) -> (A -> C)"),
                expressionParser.parseString("A -> B -> A&B"),
                expressionParser.parseString("A&B -> A"),
                expressionParser.parseString("A&B -> B"),
                expressionParser.parseString("A -> A | B"),
                expressionParser.parseString("B -> A | B"),
                expressionParser.parseString("(A -> C) -> (B -> C) -> (A | B -> C)"),
                expressionParser.parseString("(A -> B) -> (A -> !B) -> !A"),
                expressionParser.parseString("!!A -> A")
        };
    }

    private static final String MODUS_PONENS_LOG = "MP %d %d";
    private static final String AXIOM_LOG = "AX %d";
    private static final boolean DEBUG = false;

    ExpressionParser expressionParser = new ExpressionParser();

    private static final class ExpressionNum {
        final Expression expression;
        final int num;

        private ExpressionNum(Expression expression, int num) {
            this.num = num;
            this.expression = expression;
        }
    }

    public static void main(String[] args) throws IOException {
        new ProofParser().doDeduction();
    }

    void doDeduction() throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out))) {
            String description = reader.readLine();
            String[] parts = splitDescriptionAndWriteNew(description, writer);
            Set<Expression> context = Arrays.stream(parts)
                    .limit(parts.length - 2)
                    .map(expressionParser::parseString)
                    .collect(Collectors.toSet());
            Expression alpha = expressionParser.parseString(
                    parts[parts.length - 2]), beta = expressionParser.parseString(parts[parts.length - 1]);
            writer.write(String.format("|- %s -> %s%n", alpha, beta));
            String statement;
            Map<Expression, Integer> previous = new HashMap<>();
            Map<Expression, List<ExpressionNum>> prevImplRightPart = new HashMap<>();
            for (int num = 0; (statement = reader.readLine()) != null; ) {
                int numAxiom;
                Expression deltaI = expressionParser.parseString(statement);
                if (deltaI.equals(alpha)) {
                    for (String sentence : proveIdentity(alpha)) {
                        if (DEBUG) {
                            writer.write(String.format("%-4d%s%n", num++, sentence));
                        } else {
                            writer.write(sentence);
                            writer.newLine();
                            num++;
                        }
                    }
                } else if (context.contains(deltaI)) {
                    for (String sentence : proveAxiomOrHypothesis(deltaI, alpha, -1)) {
                        if (DEBUG) {
                            writer.write(String.format("%-4d%s%n", num++, sentence));
                        } else {
                            writer.write(sentence);
                            writer.newLine();
                            num++;
                        }
                    }
                } else if ((numAxiom = isAxiomNum(deltaI)) != -1) {
                    for (String sentence : proveAxiomOrHypothesis(deltaI, alpha, numAxiom)) {
                        if (DEBUG) {
                            writer.write(String.format("%-4d%s%n", num++, sentence));
                        } else {
                            writer.write(sentence);
                            writer.newLine();
                            num++;
                        }
                    }
                } else {
                    int prevNum = num;
                    List<ExpressionNum> expressionNums = prevImplRightPart.get(deltaI);
                    for (ExpressionNum expressionNum : expressionNums) {
                        if (previous.containsKey(expressionNum.expression)) {
                            String[] proof = proveModusPonens(deltaI, expressionNum.expression, alpha,
                                    previous.get(expressionNum.expression), expressionNum.num);
                            for (String sentence : proof) {
                                if (DEBUG) {
                                    writer.write(String.format("%-4d%s%n", num++, sentence));
                                } else {
                                    writer.write(sentence);
                                    writer.newLine();
                                    num++;
                                }
                            }
                            break;
                        }
                    }
                    if (num == prevNum) {
                        throw new AssertionError("Modus ponens not found");
                    }
                }
                previous.put(deltaI, num - 1);
                if (deltaI instanceof Impl) {
                    if (prevImplRightPart.containsKey(((Impl) deltaI).right)) {
                        prevImplRightPart.get(((Impl) deltaI).right)
                                .add(new ExpressionNum(((Impl) deltaI).left, num - 1));
                    } else {
                        List<ExpressionNum> newList = new ArrayList<>();
                        newList.add(new ExpressionNum(((Impl) deltaI).left, num - 1));
                        prevImplRightPart.put(((Impl) deltaI).right, newList);
                    }
                }
                if (deltaI.equals(beta)) {
                    break;
                }
            }
        }
    }

    private static String[] splitDescriptionAndWriteNew(String description, BufferedWriter writer) throws IOException {
        String[] parts = description.split("\\s*(,|\\|-)\\s*");
        writer.write(Arrays.stream(parts).limit(parts.length - 2).collect(Collectors.joining(", ")));
        return parts;
    }

    private String[] proveIdentity(Expression alpha) {
        String[] result = new String[]{
                String.format("%1$s -> %1$s -> %1$s", alpha),
                String.format("(%1$s -> %1$s -> %1$s) -> (%1$s -> (%1$s -> %1$s) -> %1$s) -> (%1$s -> %1$s)", alpha),
                String.format("(%1$s -> (%1$s -> %1$s) -> %1$s) -> (%1$s -> %1$s)", alpha),
                String.format("(%1$s -> (%1$s -> %1$s) -> %1$s)", alpha),
                String.format("%1$s -> %1$s", alpha)
        };
        if (DEBUG) {
            result[0] = String.format("%-11s%s", String.format(AXIOM_LOG, 1), result[0]);
            result[1] = String.format("%-11s%s", String.format(AXIOM_LOG, 2), result[1]);
            result[2] = String.format("%-11s%s", String.format(MODUS_PONENS_LOG, -1, -2), result[2]);
            result[3] = String.format("%-11s%s", String.format(AXIOM_LOG, 1), result[3]);
            result[4] = String.format("%-11s%s", String.format(MODUS_PONENS_LOG, -2, -1), result[4]);
        }
        return result;
    }

    private String[] proveModusPonens(
            Expression deltaI,
            Expression deltaJ,
            Expression alpha,
            int alphaImplDeltaJNum,
            int alphaImplDeltaKNum
    ) {
        String[] result = {
                String.format("(%1$s -> %2$s) -> (%1$s -> %2$s -> %3$s) -> (%1$s -> %3$s)", alpha, deltaJ, deltaI),
                String.format("(%1$s -> %2$s -> %3$s) -> (%1$s -> %3$s)", alpha, deltaJ, deltaI),
                String.format("%s -> %s", alpha, deltaI)
        };
        if (DEBUG) {
            result[0] = String.format("%-11s%s", String.format(AXIOM_LOG, 2), result[0]);
            result[1] = String.format("%-11s%s", String.format(MODUS_PONENS_LOG, -1, alphaImplDeltaJNum), result[1]);
            result[2] = String.format("%-11s%s", String.format(MODUS_PONENS_LOG, -1, alphaImplDeltaKNum), result[2]);
        }
        return result;
    }

    private String[] proveAxiomOrHypothesis(
            Expression deltaI,
            Expression alpha,
            int axiomNum
    ) throws IOException {
        String[] result = {
                deltaI.toString(),
                String.format("%1$s -> %2$s -> %1$s", deltaI, alpha),
                String.format("%s -> %s", alpha, deltaI)
        };
        if (DEBUG) {
            result[0] = String.format("%-11s%s", String.format(AXIOM_LOG, axiomNum), result[0]);
            result[1] = String.format("%-11s%s", String.format(AXIOM_LOG, 1), result[1]);
            result[2] = String.format("%-11s%s", String.format(MODUS_PONENS_LOG, -1, -2), result[2]);
        }
        return result;
    }

    int isAxiomNum(Expression e) {
        Map<String, Expression> match = new HashMap<>();
        for (int i = 0; i < AXIOMS.length; i++) {
            match.clear();
            if (AXIOMS[i].matchAxiom(e, match)) {
                return i + 1;
            }
        }
        return -1;
    }
}
