/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.doris.rest.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.seatunnel.connectors.doris.rest.models.Field;

public class Schema {
    private int status = 0;
    private String keysType;
    private List<Field> properties;

    public Schema() {
        this.properties = new ArrayList<Field>();
    }

    public Schema(int fieldCount) {
        this.properties = new ArrayList<Field>(fieldCount);
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getKeysType() {
        return this.keysType;
    }

    public void setKeysType(String keysType) {
        this.keysType = keysType;
    }

    public List<Field> getProperties() {
        return this.properties;
    }

    public void setProperties(List<Field> properties) {
        this.properties = properties;
    }

    public void put(String name, String type, String comment, int scale, int precision, String aggregationType) {
        this.properties.add(new Field(name, type, comment, scale, precision, aggregationType));
    }

    public void put(Field f) {
        this.properties.add(f);
    }

    public Field get(int index) {
        if (index >= this.properties.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Fields size:" + this.properties.size());
        }
        return this.properties.get(index);
    }

    public int size() {
        return this.properties.size();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        Schema schema = (Schema)o;
        return this.status == schema.status && Objects.equals(this.properties, schema.properties);
    }

    public int hashCode() {
        return Objects.hash(this.status, this.properties);
    }

    public String toString() {
        return "Schema{status=" + this.status + ", properties=" + this.properties + '}';
    }
}

