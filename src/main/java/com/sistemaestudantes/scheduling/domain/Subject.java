package com.sistemaestudantes.scheduling.domain;

import java.awt.Color;
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
        this.hexColor = hexColor != null ? hexColor : "#3498DB";
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

    public Color getAwtColor() {
        try {
            return Color.decode(hexColor);
        } catch (Exception e) {
            return new Color(52, 152, 219);
        }
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
