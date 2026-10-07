package com.sistemaestudantes.scheduling.domain;

import java.util.Objects;

/**
 * Representa uma disciplina ou matéria de estudo.
 */
public class Subject {
    private final String id;
    private final String name;
    private final String code;
    private final String hexColor;

    public Subject(String id, String name, String code, String hexColor) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.hexColor = hexColor != null ? hexColor : "#3A7D8C";
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public String getHexColor() {
        return hexColor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Subject subject = (Subject) o;
        return Objects.equals(id, subject.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name;
    }
}
