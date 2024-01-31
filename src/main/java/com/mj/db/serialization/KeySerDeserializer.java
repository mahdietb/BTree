package com.mj.db.serialization;

import com.mj.bplustree.fields.Field;
import com.mj.bplustree.fields.FieldType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KeySerDeserializer {

    private List<String> keySpec;
    private Map<String, Field> recordSpecMap;

    public KeySerDeserializer(Map<String, Field> recordSpec, List<String> keySpec) {

        this.keySpec = keySpec ;
        this.recordSpecMap = recordSpec ;

    }

    public  List<Object> read(DataInputStream dis) throws IOException {

        List<Object> ret = new ArrayList<>() ;

        for (String fieldName : keySpec) {

            FieldType type = recordSpecMap.get(fieldName).getFieldType();

            RecordSerDeserializer.identify(dis, ret, type);

        }

        return ret ;
    }

    public void write(List <Object>values, DataOutputStream dos) throws IOException {

        int index = 0;
        for (String fieldName : keySpec) {
            FieldType type = recordSpecMap.get(fieldName).getFieldType();

            if (type.equals(FieldType.integer)) {
                dos.writeInt((int)values.get(index));
                index++;
            } else if (type.equals(FieldType.string)){
                // String
                int len = recordSpecMap.get(fieldName).getLength();
                String val = (String)values.get(index);
                byte[] valBytes = val.getBytes(StandardCharsets.UTF_8);
                dos.writeInt(valBytes.length);
                dos.write(valBytes);
                index++ ;
            } else index = RecordSerDeserializer.getIndex(values, dos, index, type);
        }
    }
}
