package com.craftinginterpreters.language;

class Environment {
    final Environment enclosing;
    // Storing values as a list of tuples (String name and the Object it "maps" to).
    private final Object[][] values = new Object[1000][2];
    private int counter = 0;

    Environment() {
        enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    Object get(Token name) {
        int i = 0;
        while (values[i][0] != null && i < 1000) {
            if (values[i][0].equals(name.lexeme)) {
                return values[i][1];
            }
            i++;
        }

        if (enclosing != null) return enclosing.get(name);

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    void assign(Token name, Object value) {
        int i = 0;
        while (values[i][0] != null && i < 1000) {
            if (values[i][0].equals(name.lexeme)) {
                values[i][1] = value;
                return;
            }
            i++;
        }

        if (enclosing != null) enclosing.assign(name, value);
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    void define(String name, Object value) {
        int ourValue = inList(name);
        if (ourValue != -1) {
            values[ourValue][1] = value;
        }
        else {
            values[counter][0] = name;
            values[counter][1] = value;
            counter++;
        }
    }

    int inList(String name) {
        int i = 0;
        while (values[i][0] != null && i < 1000) {
            if (values[i][0].equals(name)) {
                return i;
            }
            i++;
        }

        if (enclosing != null) {
            return this.enclosing.inList(name);
        }
        return -1;
    }

    Object getAt(int distance, int index) {
        return ancestor(distance).values[index][1];
    }

    Object getAt(int distance, String name) {
        return ancestor(distance).get(new Token(TokenType.STRING, name, "", 0));
    }

    void assignAt(int distance, int index, Token name, Object value) {
        ancestor(distance).values[index] = new Object[]{name.lexeme, value};
    }

    Environment ancestor(int distance) {
        Environment environment = this;
        for (int i = 0; i < distance; i++) {
            environment = environment.enclosing;
        }

        return environment;
    }
}
