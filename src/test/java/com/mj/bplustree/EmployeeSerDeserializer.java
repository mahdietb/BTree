package com.mj.bplustree;

import com.mj.db.serialization.SerDeserializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class EmployeeSerDeserializer implements SerDeserializer<Employee> {

    public Employee read(DataInputStream dis) throws IOException {

        int id = dis.readInt();
        String fn = dis.readUTF();
        String ln = dis.readUTF();

        return new Employee(id, fn, ln);
    }

    public void write(Employee value, DataOutputStream dos) throws IOException {

        dos.writeInt(value.getId());
        dos.writeUTF(value.getFirstName());
        dos.writeUTF(value.getLastName());

    }

}
