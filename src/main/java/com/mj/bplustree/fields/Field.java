package com.mj.bplustree.fields;

public class Field {
    private String name;
    private FieldType fieldType;
    private int length;

    public Field(String name, FieldType fieldType, int length) {
        this.name = name;
        this.fieldType = fieldType;
        this.length = length;
    }

    public Field(String name, FieldType fieldType) {
        this.name = name;
        this.fieldType = fieldType;
    }

    public String getName() {
        return name ;
    }

    public FieldType getFieldType() {
        return fieldType;
    }

    public int getLength() {
        return length;
    }

    public int getSize() {

        switch(fieldType) {
            case integer:
            case decimal:
                return 4;
            case string:
                return length *2;
            case bool:
                return 1;
            default:
                return 8;

        }

    }
}
