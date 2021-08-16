package com.craftinginterpreters.language;

import java.util.List;
import java.util.Map;

public class LanguageClass implements LanguageCallable {
    final String name;
    private final Map<String, LanguageFunction> methods;

    LanguageClass(String name, Map<String, LanguageFunction> methods) {
        this.name = name;
        this.methods = methods;
    }

    LanguageFunction findMethod(String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }

        return null;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        LanguageInstance instance = new LanguageInstance(this);
        LanguageFunction initializer = findMethod("init");
        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments);
        }
        return instance;
    }

    @Override
    public int arity() {
        LanguageFunction initializer = findMethod("init");
        if (initializer == null) return 0;
        return initializer.arity();
    }
}
