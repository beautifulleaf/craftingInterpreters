package com.craftinginterpreters.language;

import java.util.List;

interface LanguageCallable {
    int arity();
    Object call(Interpreter interpreter, List<Object> arguments);
}
