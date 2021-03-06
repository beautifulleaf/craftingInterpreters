package com.craftinginterpreters.language;

class AstPrinter implements Expr.Visitor<String>{
    String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "nil";
        return expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    @Override
    public String visitTernaryExpr(Expr.Ternary expr) {
        return parenthesize("?:", expr.condition, expr.result, expr.altResult);
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return expr.name.toString();
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) { return expr.name.toString() + ", " + expr.value.toString(); }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) { return expr.left.toString() + ", " + expr.operator.toString() + ", "
    + expr.right.toString(); }

    @Override
    public String visitCallExpr(Expr.Call expr) {return expr.callee.toString() + ", " + expr.paren.toString() +
            ", " + expr.arguments.toString(); }

    @Override
    public String visitGetExpr(Expr.Get expr) {
        return expr.type;
    }

    @Override
    public String visitSetExpr(Expr.Set expr) {
        return expr.type;
    }

    @Override
    public String visitThisExpr(Expr.This expr) {return expr.type;}

    @Override
    public String visitSuperExpr(Expr.Super expr) {return expr.type;}
    /*
    @Override
    public String visitLambdaExpr(Expr.Lambda expr) {return expr.parameters.toString() + ", " + expr.body.toString();}
    */
    private String parenthesize(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();

        builder.append("(").append(name);
        for (Expr expr : exprs) {
            builder.append(" ");
            builder.append(expr.accept(this));
        }
        builder.append(")");

        return builder.toString();
    }
}
