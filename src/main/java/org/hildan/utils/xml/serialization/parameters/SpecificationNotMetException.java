package org.hildan.utils.xml.serialization.parameters;

@SuppressWarnings("serial")
public class SpecificationNotMetException extends Exception {
    public SpecificationNotMetException(String s) {
        super(s);
    }
}