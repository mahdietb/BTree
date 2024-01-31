/**
 * 
 */
package com.mj.db.serialization;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class IntegerSerDeserializer implements SerDeserializer<Integer> {
	
	public Integer read(DataInputStream dis) throws IOException {
		return dis.readInt();
	}

	public void write(Integer value, DataOutputStream dos) throws IOException {
		dos.writeInt(value) ;
	}
	
	
	
	
}
