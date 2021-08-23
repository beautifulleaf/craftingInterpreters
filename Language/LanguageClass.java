package com.craftinginterpreters.language;

import java.util.List;
import java.util.Map;

public class LanguageClass implements LanguageCallable {
    final String name;
    final LanguageClass superclass;
    private final Map<String, LanguageFunction> methods;

    LanguageClass(String name, LanguageClass superclass, Map<String, LanguageFunction> methods) {
        this.superclass = superclass;
        this.name = name;
        this.methods = methods;
    }

    LanguageFunction findMethod(String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }

        if (superclass != null) {
            return superclass.findMethod(name);
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
