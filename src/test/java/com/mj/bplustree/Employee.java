package com.mj.bplustree;

public class Employee {

    private int id;
    private String firstName;
    private String lastName;


    public Employee(int i, String fn, String ln) {
        id = i;
        firstName = fn;
        lastName = ln;
    }

    public int getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {

        return lastName;

    }

    @Override
    public String toString() {
        return lastName +
                ' ' +
                firstName +
                ' ' +
                id;
    }
}
