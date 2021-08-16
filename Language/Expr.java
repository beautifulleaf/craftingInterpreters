package com.craftinginterpreters.language;

import java.util.List;

abstract class Expr {
    interface Visitor<R> {
        R visitBinaryExpr(Binary expr);
        R visitGroupingExpr(Grouping expr);
        R visitLiteralExpr(Literal expr);
        R visitUnaryExpr(Unary expr);
        R visitTernaryExpr(Ternary expr);
        R visitVariableExpr(Variable expr);
        R visitAssignExpr(Assign expr);
        R visitLogicalExpr(Logical expr);
        R visitCallExpr(Call expr);
       // R visitLambdaExpr(Lambda expr);
    }

    String type;
    Integer index = -1;
    Integer distance = -1;

    void setDistance(int distance) {
        this.distance = distance;
    }

    void setIndex(int index) {
        this.index = index;
    }

    static class Binary extends Expr {
        Binary(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
            type = "binary";
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitBinaryExpr(this);
        }

        final Expr left;
        final Token operator;
        final Expr right;
    }
    static class Grouping extends Expr {
        Grouping(Expr expression) {
            this.expression = expression; type = "grouping";
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitGroupingExpr(this);
        }

        final Expr expression;
    }
    static class Literal extends Expr {
        Literal(Object value) {
            this.value = value;type = "literal";
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitLiteralExpr(this);
        }

        final Object value;
    }
    static class Unary extends Expr {
        Unary(Token operator, Expr right) {
            this.operator = operator;
            this.right = right;
            type = "unary";
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitUnaryExpr(this);
        }

        final Token operator;
        final Expr right;
    }
    static class Ternary extends Expr {
        Ternary(Expr condition, Expr result, Expr altResult) {
            this.condition = condition;
            this.result = result;
            this.altResult = altResult;
            type = "ternary";
        }

        @Override
        <R> R accept(Visitor<R> visitor) { return visitor.visitTernaryExpr(this); }

        final Expr condition;
        final Expr result;
        final Expr altResult;
    }

    static class Variable extends Expr {
        Variable(Token name) {
            this.name = name;
            type = "variable";
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitVariableExpr(this);
        }

        final Token name;
    }

    static class Assign extends Expr {
        Assign(Token name, Expr value) {
            this.name = name;
            this.value = value;
            type = "assign";
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitAssignExpr(this);
        }

        final Token name;
        final Expr value;
    }

    static class Logical extends Expr {
        Logical(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
            type = "logical";
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitLogicalExpr(this);
        }

        final Expr left;
        final Token operator;
        final Expr right;
    }

    static class Call extends Expr {
        Call(Expr callee, Token paren, List<Expr> arguments) {
            this.callee = callee;
            this.paren = paren;
            this.arguments = arguments;
            type = "call";
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitCallExpr(this);
        }

        final Expr callee;
        final Token paren;
        final List<Expr> arguments;
    }
    /*
    static class Lambda extends Expr implements LanguageCallable {
        Lambda(List<Token> parameters, List<Stmt> body) {
            this.parameters = parameters;
            this.body = body;
            type = "lambda";
        }

        @Override
        <R> R accept(Visitor<R> visitor) {return visitor.visitLambdaExpr(this);}

        final List<Token> parameters;
        final List<Stmt> body;
    } */

    abstract <R> R accept(Visitor <R> visitor);
}
