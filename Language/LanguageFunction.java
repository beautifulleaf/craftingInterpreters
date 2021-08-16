package com.craftinginterpreters.language;

import java.util.List;

class LanguageFunction implements LanguageCallable {
    private final Stmt.Function declaration;
    private final Environment closure;
    private final boolean isInitializer;

    LanguageFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
        this.isInitializer = isInitializer;
        this.closure = closure;
        this.declaration = declaration;
    }

    LanguageFunction bind(LanguageInstance instance) {
        Environment environment = new Environment(closure);
        environment.define("this", instance);
        return new LanguageFunction(declaration, environment, isInitializer);
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);
        for (int i = 0; i < declaration.params.size(); i++) {
            environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }

        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            Token dummyToken = new Token(TokenType.THIS, "this", "", 0);
            if (isInitializer) return closure.get(dummyToken);
            return returnValue.value;
        }

        Token dummyToken = new Token(TokenType.THIS, "this", "", 0);
        if (isInitializer) return closure.get(dummyToken);
        return null;
    }
}
