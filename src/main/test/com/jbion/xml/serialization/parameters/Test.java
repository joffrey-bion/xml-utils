package com.jbion.xml.serialization.parameters;

import java.io.IOException;

import org.xml.sax.SAXException;

import org.hildan.utils.xml.serialization.parameters.Parameters;
import org.hildan.utils.xml.serialization.parameters.ParamsSchema;
import org.hildan.utils.xml.serialization.parameters.SpecificationNotMetException;
import org.hildan.utils.xml.serialization.serializers.ArraySerializer;
import org.hildan.utils.xml.serialization.serializers.SimpleSerializer;

public class Test {

    /**
     * Test.
     * 
     * @param args
     *            ignored.
     */
    public static void main(String[] args) {
        ParamsSchema schema = new ParamsSchema("test", 1);
        schema.addParam("myBool", SimpleSerializer.BOOLEAN);
        schema.addOptionalParam("myOptionalDouble", SimpleSerializer.DOUBLE, 4.0);
        schema.addOptionalParam("myRequiredDouble", SimpleSerializer.DOUBLE, 4.0);
        schema.addOptionalParam("array", ArraySerializer.INTEGER_ARRAY, new Integer[] { 1, 2 });
        System.out.println(schema);
        System.out.println();
        System.out.println("Parameters version 1:");
        Parameters p = new Parameters(schema);
        p.set("myBool", null);
        p.set("myRequiredDouble", 9.0);
        p.toString();
        System.out.println();
        ParamsSchema schema2 = new ParamsSchema("test", 2);
        schema2.addAll(schema);
        schema2.addOptionalParam("sarray", ArraySerializer.STRING_ARRAY, new String[] { "a,", "b\\",
                "c,d" });
        schema2.addParam("co", new MyObjectSerializer(), "this is a complex object");
        System.out.println(schema2);
        System.out.println();
        System.out.println("Parameters version 2:");
        Parameters p2 = new Parameters(schema, schema2);
        p2.set("myBool", null);
        p2.set("myRequiredDouble", 9.0);
        p2.set("co", new MyObject(true, "blabla", new Integer[] { 1, 5, 8 }));
        p2.toString();
        String home = System.getProperty("user.home");
        try {
            p.saveToXml(home + "/Desktop/params.xml");
            p2.saveToXml(home + "/Desktop/params2.xml");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SpecificationNotMetException e) {
            e.printStackTrace();
        }
        System.out.println();
        try {
            p.loadFromXml(home + "/Desktop/params.xml");
            p.toString();
            System.out.println();
            p2.loadFromXml(home + "/Desktop/params.xml");
            p2.toString();
            System.out.println();
            p2.loadFromXml(home + "/Desktop/params2.xml");
            p2.toString();
        } catch (IOException | SAXException | SpecificationNotMetException e) {
            e.printStackTrace();
        }
    }

}
