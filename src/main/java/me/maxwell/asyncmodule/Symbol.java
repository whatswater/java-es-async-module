package me.maxwell.asyncmodule;

import java.util.Objects;

public class Symbol<T> implements Comparable<Symbol<T>> {
    private final String key;

    public Symbol(String key) {
        this.key = key;
    }

    @Override
    public int compareTo(Symbol<T> o) {
        return this.key.compareTo(o.key);
    }

    public String getKey() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }

        if(o == null || this.getClass() != o.getClass()) {
            return false;
        }
        Symbol<?> symbol = (Symbol<?>) o;
        return Objects.equals(key, symbol.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public String toString() {
        return String.format("symbol %s", key);
    }
}
