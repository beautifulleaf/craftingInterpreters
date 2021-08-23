package com.craftinginterpreters.language;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    final Environment globals = new Environment();
    private Environment environment = globals;
    private static final Map<Expr, Integer[]> locals = new HashMap<>();
    private static boolean isBreak = false;

    Interpreter() {
        globals.define("clock", new LanguageCallable() {
            @Override
            public int arity() {return 0;}

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double)System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });
    }

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Language.runtimeError(error);
        }
    }

    void interpretEx(Expr expression) {
        try {
            Object value = evaluate(expression);
            System.out.println(stringify(value));
        } catch (RuntimeError error) {
            Language.runtimeError(error);
        }
    }

    private void execute(Stmt stmt) {
        if (stmt.type.equals("break")) isBreak = true;
        stmt.accept(this);
    }

    void resolve(Expr expr, int depth, int index) {
        Integer[] locations = {depth, index};
        locals.put(expr, locations);
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        Object superclass = null;
        if (stmt.superclass != null) {
            superclass = evaluate(stmt.superclass);
            if (!(superclass instanceof LanguageClass)) {
                throw new RuntimeError(stmt.superclass.name, "Superclass must be a class.");
            }
        }

        environment.define(stmt.name.lexeme, null);

        if (stmt.superclass != null) {
            environment = new Environment(environment);
            environment.define("super", superclass);
        }

        Map<String, LanguageFunction> methods = new HashMap<>();
        for (Stmt.Function method : stmt.methods) {
            LanguageFunction function = new LanguageFunction(method, environment, method.name.lexeme.equals("init"));
            methods.put(method.name.lexeme, function);
        }

        LanguageClass klass = new LanguageClass(stmt.name.lexeme, (LanguageClass)superclass, methods);

        if (superclass != null) {
            environment = environment.enclosing;
        }

        environment.assign(stmt.name, klass);
        return null;
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }

    @Override
    public Object visitSetExpr(Expr.Set expr) {
        Object object = evaluate(expr.object);

        if (!(object instanceof LanguageInstance)) {
            throw new RuntimeError(expr.name, "Only instance have fields.");
        }

        Object value = evaluate(expr.value);
        ((LanguageInstance)object).set(expr.name, value);
        return value;
    }

    @Override
    public Object visitSuperExpr(Expr.Super expr) {
        int distance = locals.get(expr)[0];
        LanguageClass superclass = (LanguageClass)environment.getAt(distance, "super");
        LanguageInstance object = (LanguageInstance)environment.getAt(distance - 1, "this");
        LanguageFunction method = superclass.findMethod(expr.method.lexeme);

        if (method == null) {
            throw new RuntimeError(expr.method, "Undefined property '" + expr.method.lexeme + "'.");
        }

        return method.bind(object);
    }

    @Override
    public Object visitThisExpr(Expr.This expr) {
        return lookUpVariable(expr.keyword, expr);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
        }

        // Unreachable.
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookUpVariable(expr.name, expr);
    }

    private Object lookUpVariable(Token name, Expr expr) {
        Integer[] locationValues = locals.get(expr);
        if (locationValues != null) {
            return environment.getAt(locationValues[0], locationValues[1]);
        } else {
            return globals.get(name);
        }
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;

        throw new RuntimeError(operator, "Operands must be number.");
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;

        return a.equals(b);
    }

    private String stringify(Object object) {
        if (object == null) return "nil";
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }
        return object.toString();
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        LanguageFunction function = new LanguageFunction(stmt, environment, false);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) value = evaluate(stmt.value);

        throw new Return(value);
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }
        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            if (isBreak) {isBreak = false; break;}
            execute(stmt.body);
        }
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        return null;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        Integer[] locationValues = locals.get(expr);
        if (locationValues != null) {
            environment.assignAt(locationValues[0], locationValues[1], expr.name, value);
        } else {
            globals.assign(expr.name, value);
        }
        return value;
    }

    /* @Override
    public Object visitLambdaExpr(Expr.Lambda expr) {
        for (Stmt stmt : expr.body) {
            execute(stmt);
        }
        return null;
    } */
    
    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case COMMA:
                // Left and right are already evaluated at this point, so we just need to return right.
                return right;
            case GREATER:
                if (left instanceof String && right instanceof String) {
                    return ((String) left).compareTo((String) right) > 0;
                }
                if (left instanceof String && right instanceof Double) {
                    String r = Double.toString((double)right);
                    return ((String) left).compareTo(r) > 0;
                }
                if (left instanceof Double && right instanceof String) {
                    String r = Double.toString((double)left);
                    return (r).compareTo((String) right) > 0;
                }
                if (left instanceof Double && right instanceof Double) {
                    checkNumberOperands(expr.operator, left, right);
                    return (double)left > (double)right;
                }
                throw new RuntimeError(expr.operator, "Operands must be numbers or strings.");
            case GREATER_EQUAL:
                if (left instanceof String && right instanceof String) {
                    return ((String) left).compareTo((String) right) >= 0;
                }
                if (left instanceof String && right instanceof Double) {
                    String r = Double.toString((double)right);
                    return ((String) left).compareTo(r) >= 0;
                }
                if (left instanceof Double && right instanceof String) {
                    String r = Double.toString((double)left);
                    return (r).compareTo((String) right) >= 0;
                }
                if (left instanceof Double && right instanceof Double) {
                    checkNumberOperands(expr.operator, left, right);
                    return (double)left >= (double)right;
                }
                throw new RuntimeError(expr.operator, "Operands must be numbers or strings.");
            case LESS:
                if (left instanceof String && right instanceof String) {
                    return ((String) left).compareTo((String) right) < 0;
                }
                if (left instanceof String && right instanceof Double) {
                    String r = Double.toString((double)right);
                    return ((String) left).compareTo(r) < 0;
                }
                if (left instanceof Double && right instanceof String) {
                    String r = Double.toString((double)left);
                    return (r).compareTo((String) right) < 0;
                }
                if (left instanceof Double && right instanceof Double) {
                    checkNumberOperands(expr.operator, left, right);
                    return (double)left < (double)right;
                }
                throw new RuntimeError(expr.operator, "Operands must be numbers or strings.");
            case LESS_EQUAL:
                if (left instanceof String && right instanceof String) {
                    return ((String) left).compareTo((String) right) <= 0;
                }
                if (left instanceof String && right instanceof Double) {
                    String r = Double.toString((double)right);
                    return ((String) left).compareTo(r) <= 0;
                }
                if (left instanceof Double && right instanceof String) {
                    String r = Double.toString((double)left);
                    return (r).compareTo((String) right) <= 0;
                }
                if (left instanceof Double && right instanceof Double) {
                    checkNumberOperands(expr.operator, left, right);
                    return (double)left <= (double)right;
                }
                throw new RuntimeError(expr.operator, "Operands must be numbers or strings.");
            case BANG_EQUAL: return !isEqual(left, right);
            case EQUAL_EQUAL: return isEqual(left, right);
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                if ((double)right == 0) throw new RuntimeError(expr.operator, "Cannot divide by zero.");
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }
                if (left instanceof String && !(right instanceof String)) {
                    return (String)left + String.valueOf(right);
                }

                if (!(left instanceof String) && right instanceof String) {
                    return String.valueOf(left) + (String)right;
                }
                if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
        }
        // Unreachable.
        return null;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);
        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof LanguageCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }

        LanguageCallable function = (LanguageCallable)callee;
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected " + function.arity() +
                    " arguments but got " + arguments.size() + ".");
        }
        return function.call(this, arguments);
    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
        Object object = evaluate(expr.object);
        if (object instanceof LanguageInstance) {
            return ((LanguageInstance) object).get(expr.name);
        }

        throw new RuntimeError(expr.name, "Only instances have properties.");
    }

    @Override
    public Object visitTernaryExpr(Expr.Ternary expr) {
        Object condition = evaluate(expr.condition);
        Object result = evaluate(expr.result);
        Object altResult = evaluate(expr.altResult);
        if (isTruthy(condition)) return result;
        else return altResult;
    }
}
