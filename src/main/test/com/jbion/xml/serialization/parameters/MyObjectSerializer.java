package com.jbion.xml.serialization.parameters;

import java.util.HashMap;

import org.hildan.utils.xml.serialization.serializers.ArraySerializer;
import org.hildan.utils.xml.serialization.serializers.ObjectSerializer;
import org.hildan.utils.xml.serialization.serializers.Serializer;
import org.hildan.utils.xml.serialization.serializers.SimpleSerializer;

class MyObjectSerializer extends ObjectSerializer<MyObject> {

    public MyObjectSerializer() {
        super(MyObject.class);
    }

    @Override
    protected HashMap<String, Serializer<?>> getFieldsSpec() {
        HashMap<String, Serializer<?>> spec = new HashMap<>();
        spec.put("b", SimpleSerializer.BOOLEAN);
        spec.put("str", SimpleSerializer.STRING);
        spec.put("array", ArraySerializer.INTEGER_ARRAY);
        return spec;
    }

    @Override
    protected HashMap<String, Object> getFieldsValues(MyObject object) {
        HashMap<String, Object> fields = new HashMap<>();
        fields.put("b", object.b);
        fields.put("str", object.str);
        fields.put("array", object.array);
        return fields;
    }

    @Override
    protected MyObject createInstanceFromFields(HashMap<String, Object> fields) {
        MyObject o = new MyObject();
        o.b = (boolean) fields.get("b");
        o.str = (String) fields.get("str");
        o.array = (Integer[]) fields.get("array");
        return o;
    }

}
