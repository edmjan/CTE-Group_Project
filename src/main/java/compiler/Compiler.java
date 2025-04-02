package compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

enum TokenType {
    KEYWORD, IDENTIFIER, OPERATOR, LITERAL, WHITESPACE, ERROR, SEMICOLON, EQUALS
}

class Token {
    TokenType type;
    String value;

    Token(TokenType type, String value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public String toString() {
        return "<" + type + ", " + value + ">";
    }
}

class Lexer {
    private String input;
    private int position;

    Lexer(String input) {
        this.input = input;
        this.position = 0;
    }

    List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (position < input.length()) {
            char currentChar = input.charAt(position);
            if (Character.isWhitespace(currentChar)) {
                position++;
                continue;
            }
            if (Character.isLetter(currentChar)) {
                StringBuilder identifier = new StringBuilder();
                while (position < input.length() && Character.isLetterOrDigit(input.charAt(position))) {
                    identifier.append(input.charAt(position));
                    position++;
                }
                String identifierStr = identifier.toString();
                if (identifierStr.equals("if") || identifierStr.equals("else")) {
                    tokens.add(new Token(TokenType.KEYWORD, identifierStr));
                } else {
                    tokens.add(new Token(TokenType.IDENTIFIER, identifierStr));
                }
            } else if (currentChar == '+' || currentChar == '-' || currentChar == '*' || currentChar == '/') {
                tokens.add(new Token(TokenType.OPERATOR, String.valueOf(currentChar)));
                position++;
            } else if (Character.isDigit(currentChar)) {
                StringBuilder literal = new StringBuilder();
                while (position < input.length() && Character.isDigit(input.charAt(position))) {
                    literal.append(input.charAt(position));
                    position++;
                }
                tokens.add(new Token(TokenType.LITERAL, literal.toString()));
            } else if (currentChar == ';'){
              tokens.add(new Token(TokenType.SEMICOLON, ";"));
              position++;
            } else if (currentChar == '='){
                tokens.add(new Token(TokenType.EQUALS, "="));
                position++;
            } else {
                tokens.add(new Token(TokenType.ERROR, String.valueOf(currentChar)));
                position++;
            }
        }
        return tokens;
    }
}

abstract class Node {}

class LiteralNode extends Node {
    String value;

    LiteralNode(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Literal: " + value;
    }
}

class IdentifierNode extends Node {
    String name;

    IdentifierNode(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Identifier: " + name;
    }
}

class BinaryOpNode extends Node {
    Node left;
    String operator;
    Node right;

    BinaryOpNode(Node left, String operator, Node right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    @Override
    public String toString() {
        return "(" + left + " " + operator + " " + right + ")";
    }
}

class AssignmentNode extends Node {
    String identifier;
    Node expression;

    AssignmentNode(String identifier, Node expression) {
        this.identifier = identifier;
        this.expression = expression;
    }

    @Override
    public String toString() {
        return identifier + " = " + expression;
    }
}

class Parser {
    private List<Token> tokens;
    private int position;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.position = 0;
    }

    Node parseStatement() {
        if (match(TokenType.IDENTIFIER)) {
            String identifier = tokens.get(position - 1).value;
            if (match(TokenType.EQUALS)) {
                Node expression = parseExpression();
                if (match(TokenType.SEMICOLON)) {
                    return new AssignmentNode(identifier, expression);
                } else {
                    System.err.println("Error: Expected semicolon after assignment.");
                    return null;
                }
            } else {
                position--; // Backtrack if not an assignment
            }
        }
        return parseExpression(); // Try parsing an expression
    }

    Node parseExpression() {
        Node left = parseTerm();
        while (match(TokenType.OPERATOR) && (tokens.get(position - 1).value.equals("+") || tokens.get(position - 1).value.equals("-"))) {
            String operator = tokens.get(position - 1).value;
            Node right = parseTerm();
            left = new BinaryOpNode(left, operator, right);
        }
        return left;
    }

    Node parseTerm() {
        Node left = parseFactor();
        while (match(TokenType.OPERATOR) && (tokens.get(position - 1).value.equals("*") || tokens.get(position - 1).value.equals("/"))) {
            String operator = tokens.get(position - 1).value;
            Node right = parseFactor();
            left = new BinaryOpNode(left, operator, right);
        }
        return left;
    }

    Node parseFactor() {
        if (match(TokenType.LITERAL)) {
            return new LiteralNode(tokens.get(position - 1).value);
        } else if (match(TokenType.IDENTIFIER)) {
            return new IdentifierNode(tokens.get(position - 1).value);
        } else if (match(TokenType.KEYWORD)){
            return new IdentifierNode(tokens.get(position-1).value);
        } else {
            return null; // Error handling omitted for brevity
        }
    }

    boolean match(TokenType type) {
        if (position < tokens.size() && tokens.get(position).type == type) {
            position++;
            return true;
        }
        return false;
    }

    List<Node> parse() {
        List<Node> statements = new ArrayList<>();
        while (position < tokens.size()) {
            Node statement = parseStatement();
            if (statement != null) {
                statements.add(statement);
            }
        }
        return statements;
    }
}

class SemanticAnalyzer {
    private final Map<String, String> symbolTable = new HashMap<>(); // Identifier -> Type

    void analyze(Node node) {
        if (node instanceof IdentifierNode idNode) {
            if (!symbolTable.containsKey(idNode.name)) {
                System.err.println("Error: Undeclared identifier " + idNode.name);
            }
        } else if (node instanceof AssignmentNode assign){
            symbolTable.put(assign.identifier, "int"); //simplified type.
            analyze(assign.expression);
        } else if (node instanceof BinaryOpNode binop){
            analyze(binop.left);
            analyze(binop.right);
        }

        // ... more sophisticated type checking and symbol table operations
    }
    void analyze(List<Node> nodes){
        for(Node node : nodes){
            analyze(node);
        }
    }
}

class IntermediateCodeGenerator {
    private final List<String> intermediateCode = new ArrayList<>();

    List<String> generate(Node node) {
        if (node instanceof LiteralNode literalNode) {
            intermediateCode.add("push " + literalNode.value);
        } else if (node instanceof IdentifierNode identifierNode) {
            intermediateCode.add("load " + identifierNode.name);
        } else if (node instanceof BinaryOpNode binop){
            generate(binop.left);
            generate(binop.right);
            intermediateCode.add("operate " + binop.operator);
        } else if (node instanceof AssignmentNode assign){
            generate(assign.expression);
            intermediateCode.add("store " + assign.identifier);
        }
        return intermediateCode;
    }
    List<String> generate(List<Node> nodes){
        List<String> allCode = new ArrayList<>();
        for (Node node: nodes){
            allCode.addAll(generate(node));
        }
        return allCode;
    }
}

class CodeGenerator {
    String generateMachineCode(List<String> intermediateCode) {
        String machineCode = "";
        for (String instruction : intermediateCode) {
            machineCode += instruction + "\n";
        }
        return machineCode;
    }
}

public class Compiler {
    public static void main(String[] args) {
        String input = "x = 10; y = x + 5; z = y * 2;";

        Lexer lexer = new Lexer(input);
        List<Token> tokens = lexer.tokenize();

        System.out.println("Tokens: " + tokens);

        Parser parser = new Parser(tokens);
        List<Node> ast = parser.parse();

        System.out.println("AST: " + ast);

        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
        semanticAnalyzer.analyze(ast);

        IntermediateCodeGenerator irGenerator = new IntermediateCodeGenerator();
        List<String> intermediateCode = irGenerator.generate(ast);

        System.out.println("Intermediate Code: " + intermediateCode);

        CodeGenerator codeGenerator = new CodeGenerator();
        String machineCode = codeGenerator.generateMachineCode(intermediateCode);

        System.out.println("Machine Code: \n" + machineCode);
    }
}