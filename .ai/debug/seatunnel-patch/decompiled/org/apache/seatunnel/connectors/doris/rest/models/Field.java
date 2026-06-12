/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.doris.rest.models;

import java.util.Objects;

public class Field {
    private String name;
    private String type;
    private String comment;
    private int precision;
    private int scale;
    private String aggregationType;

    public Field() {
    }

    public Field(String name, String type, String comment, int precision, int scale, String aggregationType) {
        this.name = name;
        this.type = type;
        this.comment = comment;
        this.precision = precision;
        this.scale = scale;
        this.aggregationType = aggregationType;
    }

    public String getAggregationType() {
        return this.aggregationType;
    }

    public void setAggregationType(String aggregationType) {
        this.aggregationType = aggregationType;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getComment() {
        return this.comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getPrecision() {
        return this.precision;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }

    public int getScale() {
        return this.scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        Field field = (Field)o;
        return this.precision == field.precision && this.scale == field.scale && Objects.equals(this.name, field.name) && Objects.equals(this.type, field.type) && Objects.equals(this.comment, field.comment);
    }

    public int hashCode() {
        return Objects.hash(this.name, this.type, this.comment, this.precision, this.scale);
    }

    public String toString() {
        return "Field{name='" + this.name + '\'' + ", type='" + this.type + '\'' + ", comment='" + this.comment + '\'' + ", precision=" + this.precision + ", scale=" + this.scale + '}';
    }
}

