package org.hildan.utils.xml.serialization.parameters;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.TreeMap;

import org.hildan.utils.xml.XmlHelper;
import org.hildan.utils.xml.serialization.serializers.ArraySerializer;
import org.hildan.utils.xml.serialization.serializers.Serializer;
import org.hildan.utils.xml.serialization.serializers.SimpleSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * A {@code Parameters} object contains the values of the parameters. Every parameter
 * that was set (either during XML parsing or manually via
 * {@link #set(String, Object)}) has been checked and can be safely cast from
 * {@code Object} to its class.
 * 
 * @author <a href="mailto:joffrey.bion@gmail.com">Joffrey Bion</a>
 */
public class Parameters {

    @SuppressWarnings("serial")
    public static class UnknownKeyException extends RuntimeException {
        public UnknownKeyException(String key) {
            super("The key '" + key + "' is not in the parameters schema.");
        }
    }

    @SuppressWarnings("serial")
    public static class MissingParameterException extends RuntimeException {
        public MissingParameterException(String key) {
            super("The parameter '" + key + "' is missing.");
        }
    }

    @SuppressWarnings("serial")
    public static class ParameterTypeException extends RuntimeException {
        public ParameterTypeException(String s) {
            super(s);
        }

        public ParameterTypeException(Serializer<?> paramSpecType, Serializer<?> askedType) {
            super("Trying to access as a " + askedType.getTypeName()
                    + " a parameter specified as a " + paramSpecType.getTypeName() + ".");
        }
    }

    private static final String ATT_VERSION = "version";

    /**
     * The different schema versions that can be used by this {@code Parameters}
     * object.
     */
    private TreeMap<Integer, ParamsSchema> schemaVersions;
    /**
     * The parameters schema for this {@code Parameters} object.
     */
    private ParamsSchema schema;
    /**
     * The values containing the values of the parameters. All mappings that exist in
     * this values are guaranteed to be of the right dynamic class defined by the
     * schema.
     */
    private HashMap<String, Object> values;

    /**
     * Creates a {@code Parameters} object consistent with the schema of the latest
     * version given.
     * 
     * @param schemas
     *            The different schema versions that can be used by this
     *            {@code Parameters} object.
     */
    public Parameters(ParamsSchema... schemas) {
        this.schemaVersions = new TreeMap<>();
        for (ParamsSchema ps : schemas) {
            if (schemaVersions.containsKey(ps.version())) {
                throw new IllegalArgumentException("Cannot specify 2 schemas for the same version.");
            }
            schemaVersions.put(ps.version(), ps);
        }
        this.schema = schemaVersions.get(schemaVersions.lastKey());
        this.values = new HashMap<>();
    }

    /**
     * Returns the schema currently used by this {@code Parameters}.
     * 
     * @return The schema currently used by this {@code Parameters}.
     */
    public ParamsSchema getSchema() {
        return schema;
    }

    /**
     * Returns whether this {@code Parameters} has been defined with a schema that is
     * consistent with {@code otherSchema}.
     * 
     * @param otherSchema
     *            The {@link ParamsSchema} to check.
     * @return {@code true} if this {@code Parameters} object can be used safely as
     *         if it were defined with {@code otherSchema}.
     */
    public boolean isConsistentWith(ParamsSchema otherSchema) {
        return this.schema.isSuperSetOf(otherSchema);
    }

    /**
     * Serializes this {@link Parameters} object into the specified XML file.
     * 
     * @param xmlFilePath
     *            The path to the XML output file.
     * @throws IOException
     *             If an error occurs while writing to the file.
     * @throws SpecificationNotMetException
     *             If a parameter is required and missing.
     */
    public void saveToXml(String xmlFilePath) throws IOException, SpecificationNotMetException {
        Document doc = XmlHelper.createEmptyDomDocument();
        Element root = doc.createElement(schema.name);
        root.setAttribute(ATT_VERSION, String.valueOf(schema.version()));
        doc.appendChild(root);
        for (String key : schema.keys) {
            ParamSpec spec = getSpec(key);
            if (spec.description != null) {
                root.appendChild(doc.createComment(spec.description));
            }
            try {
                Element elt = spec.serializer.objectToXml(doc, spec.key, get(spec.key,
                        spec.serializer));
                root.appendChild(elt);
            } catch (MissingParameterException e) {
                throw (SpecificationNotMetException) new SpecificationNotMetException(e
                        .getMessage()).initCause(e);
            }
        }
        XmlHelper.writeXml(xmlFilePath, doc);
    }

    /**
     * Fills this {@link Parameters} object with the values taken from the specified
     * XML file. Chooses the right schema according to the version
     * 
     * @param xmlFilePath
     *            The path to the XML input file.
     * @throws IOException
     *             If an error occurs while reading the file.
     * @throws SAXException
     *             If any XML parse error occurs.
     * @throws SpecificationNotMetException
     *             If the XML file does not meet the schema's requirements.
     */
    public void loadFromXml(String xmlFilePath) throws IOException, SAXException,
            SpecificationNotMetException {
        Document dom = XmlHelper.getDomDocumentFromFile(xmlFilePath);
        Element root = dom.getDocumentElement();
        String versionStr = root.getAttribute(ATT_VERSION);
        if (versionStr.equals("")) {
            throw new RuntimeException(
                    "No version specified in the XML file (the root needs an attribute '"
                            + ATT_VERSION + "').");
        }
        int version = Integer.parseInt(versionStr);
        schema = schemaVersions.get(version);
        if (schema == null) {
            throw new SpecificationNotMetException(
                    "No schema available to parse this version of the parameters.");
        }
        for (String key : schema.keys) {
            ParamSpec spec = schema.specs.get(key);
            Element paramElt = XmlHelper.getFirstDirectChild(root, key);
            Object paramValue;
            if (paramElt == null) {
                if (spec.required) {
                    throw new SpecificationNotMetException("The parameter '" + spec.key
                            + "' is required and missing.");
                }
            } else {
                try {
                    paramValue = spec.serializer.xmlToObject(paramElt);
                } catch (ParseException e) {
                    throw (SpecificationNotMetException) new SpecificationNotMetException(
                            "The parameter '" + spec.key
                                    + "' does not respect the format of its expected class.")
                            .initCause(e);
                }
                set(key, paramValue);
            }
        }
    }

    /**
     * Returns a new {@link Parameters} object with the values taken from the
     * specified XML file.
     * 
     * @param xmlFilePath
     *            The path to the XML input file.
     * @param schemas
     *            The different schema versions that can be used to parse the file.
     *            They have to have a different version number.
     * @return the {@link Parameters} object corresponding to the specified XML file.
     * @throws IOException
     *             If an error occurs while reading the file.
     * @throws SAXException
     *             If any XML parse error occurs.
     * @throws SpecificationNotMetException
     *             If the XML file does not meet the schema's requirements.
     */
    public static Parameters loadFromXml(String xmlFilePath, ParamsSchema... schemas)
            throws IOException, SAXException, SpecificationNotMetException {
        Parameters params = new Parameters(schemas);
        params.loadFromXml(xmlFilePath);
        return params;
    }

    /**
     * Returns the specification for the given parameter.
     * 
     * @param key
     *            The name of the parameter to get the specification for.
     * @return the {@link ParamSpec} specification corresponding to the specified
     *         {@code key}, which may not be {@code null}.
     * @throws UnknownKeyException
     *             If there is no such key in the schema.
     */
    private ParamSpec getSpec(String key) throws UnknownKeyException {
        ParamSpec spec = schema.specs.get(key);
        if (spec == null) {
            throw new UnknownKeyException(key);
        }
        return spec;
    }

    /**
     * Returns the value of the specified parameter.
     * 
     * @param key
     *            The name of the parameter to get the value of.
     * @return the value of the specified parameter, whose dynamic type is the one
     *         specified by the schema, thus can be cast safely.
     * @throws UnknownKeyException
     *             If there is no such key in the schema.
     * @throws MissingParameterException
     *             If the parameter is required and was not set.
     */
    public Object get(String key) throws UnknownKeyException, MissingParameterException {
        ParamSpec spec = getSpec(key);
        if (!values.containsKey(key)) {
            if (spec.required) {
                throw new MissingParameterException(key);
            } else {
                values.put(key, spec.defaultValue);
            }
        }
        return values.get(key);
    }

    /**
     * Returns the value of the specified parameter.
     * 
     * @param key
     *            The name of the parameter to get the value of.
     * @param serializer
     *            A serializer to specify the expected class of the parameter.
     * @return the value of the specified parameter, whose class is consistent with
     *         the specified serializer.
     * @throws UnknownKeyException
     *             If there is no such key in the schema.
     * @throws MissingParameterException
     *             If the parameter is required and was not set.
     * @throws ParameterTypeException
     *             If the specified serializer does not handle the class of the
     *             parameter as specified by the schema.
     */
    public <T> T get(String key, Serializer<T> serializer) throws UnknownKeyException,
            MissingParameterException, ParameterTypeException {
        try {
            return serializer.cast(get(key));
        } catch (ClassCastException e) {
            throw (ParameterTypeException) new ParameterTypeException(serializer,
                    getSpec(key).serializer).initCause(e);
        }
    }

    /**
     * Sets the specified parameter's value.
     * 
     * @param key
     *            The name of the parameter to set.
     * @param value
     *            The value to assign to this parameter.
     * @return the previous value of the parameter, or {@code null} if it was never
     *         set. (A {@code null} return can also indicate that the parameter was
     *         previously set to {@code null}.)
     * @throws UnknownKeyException
     *             If {@code key} is not a parameter in the schema.
     * @throws ParameterTypeException
     *             If the {@code value}'s class does not match the specification for
     *             the parameter {@code key}.
     */
    public Object set(String key, Object value) throws UnknownKeyException, ParameterTypeException {
        ParamSpec spec = getSpec(key);
        try {
            spec.serializer.cast(value);
        } catch (ClassCastException e) {
            throw (ParameterTypeException) new ParameterTypeException(
                    "The specified object's class does not match the schema ("
                            + spec.serializer.getTypeName() + " expected).").initCause(e);
        }
        return values.put(key, value);
    }

    /**
     * Deserializes the given {@code String} and sets the specified parameter with
     * the deserialized value.
     * 
     * @param key
     *            The name of the parameter to set.
     * @param serializedValue
     *            The value to assign to this parameter, in the serialized
     *            {@link String} form.
     * @return the previous value of the parameter, or {@code null} if it was never
     *         set. (A {@code null} return can also indicate that the parameter was
     *         previously set to {@code null}.)
     * @throws UnknownKeyException
     *             If {@code key} is not a parameter in the schema.
     * @throws ParseException
     *             If the given {@code String} cannot be parsed as an object from the
     *             class specified for the parameter {@code key}.
     */
    public Object deserializeAndSet(String key, String serializedValue) throws UnknownKeyException,
            ParseException {
        ParamSpec spec = getSpec(key);
        if (spec.serializer instanceof SimpleSerializer) {
            Object value = ((SimpleSerializer<?>) spec.serializer).deserialize(serializedValue);
            return values.put(key, value);
        } else {
            throw new RuntimeException("This method deals with parameters defined with a "
                    + SimpleSerializer.class.getSimpleName() + ", '" + key + "' uses a "
                    + spec.serializer.toString());
        }
    }

    /**
     * Deserializes the given array of {@code String} and sets the specified
     * parameter with the deserialized array of values.
     * 
     * @param key
     *            The name of the parameter to set.
     * @param serializedValues
     *            The array of values to assign to this parameter, in the serialized
     *            {@link String} form.
     * @return the previous value of the parameter, or {@code null} if it was never
     *         set. (A {@code null} return can also indicate that the parameter was
     *         previously set to {@code null}.)
     * @throws UnknownKeyException
     *             If {@code key} is not a parameter in the schema.
     * @throws ParseException
     *             If the given {@code String}s cannot be parsed as an object from
     *             the class specified for the parameter {@code key}.
     */
    public Object deserializeAndSet(String key, String[] serializedValues)
            throws UnknownKeyException, ParseException {
        ParamSpec spec = getSpec(key);
        if (spec.serializer instanceof ArraySerializer) {
            Object[] array = ((ArraySerializer<?>) spec.serializer).deserialize(serializedValues);
            return values.put(key, array);
        } else {
            throw new RuntimeException("This method deals with parameters defined with a "
                    + ArraySerializer.class.getSimpleName() + ", '" + key + "' uses a "
                    + spec.serializer.getTypeName() + " serializer.");
        }
    }

    /**
     * Returns a serialized version of the specified parameter. This method can only
     * be used with parameters defined with a {@link SimpleSerializer}.
     * 
     * @param key
     *            The name of the parameter to get.
     * @return A {@code String} representing the value of the parameter.
     * @throws UnknownKeyException
     *             If there is no such key in the schema.
     * @throws MissingParameterException
     *             If the parameter is required and was not set.
     */
    public String getSerialized(String key) {
        ParamSpec spec = getSpec(key);
        if (spec.serializer instanceof SimpleSerializer) {
            SimpleSerializer<?> scs = (SimpleSerializer<?>) spec.serializer;
            return scs.serialize(get(key));
        } else {
            throw new RuntimeException("This method deals with parameters defined with a "
                    + SimpleSerializer.class.getSimpleName() + ", '" + key + "' uses a "
                    + spec.serializer.toString());
        }
    }

    /**
     * Returns a serialized version of the specified parameter. This method can only
     * be used with parameters defined with a {@link ArraySerializer}.
     * 
     * @param key
     *            The name of the parameter to get.
     * @return A {@code String} array representing the value of the parameter.
     * @throws UnknownKeyException
     *             If there is no such key in the schema.
     * @throws MissingParameterException
     *             If the parameter is required and was not set.
     */
    public String[] getSerializedArray(String key) {
        ParamSpec spec = getSpec(key);
        if (spec.serializer instanceof ArraySerializer) {
            ArraySerializer<?> as = (ArraySerializer<?>) spec.serializer;
            return as.serialize((Object[]) get(key));
        } else {
            throw new RuntimeException("This method deals with parameters defined with a "
                    + ArraySerializer.class.getSimpleName() + ", '" + key + "' uses a "
                    + spec.serializer.getTypeName() + " serializer.");
        }
    }

    /**
     * Returns the value of the specified parameter.
     * 
     * @param key
     *            The name of the parameter to get the value of.
     * @return the value of the specified parameter.
     * @throws UnknownKeyException
     *             If there is no such key in the schema.
     * @throws MissingParameterException
     *             If the parameter is required and was not set.
     * @throws ParameterTypeException
     *             If the class of the parameter as specified by the schema does not
     *             match this method's return type.
     */
    public String getString(String key) throws UnknownKeyException, MissingParameterException,
            ParameterTypeException {
        return get(key, SimpleSerializer.STRING);
    }

    /**
     * Returns the value of the specified parameter.
     * 
     * @param key
     *            The name of the parameter to get the value of.
     * @return the value of the specified parameter.
     * @throws UnknownKeyException
     *             If there is no such key in the schema.
     * @throws MissingParameterException
     *             If the parameter is required and was not set.
     * @throws ParameterTypeException
     *             If the class of the parameter as specified by the schema does not
     *             match this method's return type.
     */
    public Boolean getBoolean(String key) throws UnknownKeyException, MissingParameterException,
            ParameterTypeException {
        return get(key, SimpleSerializer.BOOLEAN);
    }

    /**
     * Returns the value of the specified parameter.
     * 
     * @param key
     *            The name of the parameter to get the value of.
     * @return the value of the specified parameter.
     * @throws UnknownKeyException
     *             If there is no such key in the schema.
     * @throws MissingParameterException
     *             If the parameter is required and was not set.
     * @throws ParameterTypeException
     *             If the class of the parameter as specified by the schema does not
     *             match this method's return type.
     */
    public Character getCharacter(String key) throws UnknownKeyException,
            MissingParameterException, ParameterTypeException {
        return get(key, SimpleSerializer.CHARACTER);
    }

    /**
     * Returns the value of the specified parameter.
     * 
     * @param key
     *            The name of the parameter to get the value of.
     * @return the value of the specified parameter.
     * @throws UnknownKeyException
     *             If there is no such key in the schema.
     * @throws MissingParameterException
     *             If the parameter is required and was not set.
     * @throws ParameterTypeException
     *             If the class of the parameter as specified by the schema does not
     *             match this method's return type.
     */
    public Double getDouble(String key) throws UnknownKeyException, MissingParameterException,
            ParameterTypeException {
        return get(key, SimpleSerializer.DOUBLE);
    }

    /**
     * Returns the value of the specified parameter.
     * 
     * @param key
     *            The name of the parameter to get the value of.
     * @return the value of the specified parameter.
     * @throws UnknownKeyException
     *             If there is no such key in the schema.
     * @throws MissingParameterException
     *             If the parameter is required and was not set.
     * @throws ParameterTypeException
     *             If the class of the parameter as specified by the schema does not
     *             match this method's return type.
     */
    public Integer getInteger(String key) throws UnknownKeyException, MissingParameterException,
            ParameterTypeException {
        return get(key, SimpleSerializer.INTEGER);
    }

    /**
     * Returns the value of the specified parameter.
     * 
     * @param key
     *            The name of the parameter to get the value of.
     * @return the value of the specified parameter.
     * @throws UnknownKeyException
     *             If there is no such key in the schema.
     * @throws MissingParameterException
     *             If the parameter is required and was not set.
     * @throws ParameterTypeException
     *             If the class of the parameter as specified by the schema does not
     *             match this method's return type.
     */
    public Long getLong(String key) throws UnknownKeyException, MissingParameterException,
            ParameterTypeException {
        return get(key, SimpleSerializer.LONG);
    }

    /**
     * Returns the value of the specified parameter.
     * 
     * @param key
     *            The name of the parameter to get the value of.
     * @return the value of the specified parameter.
     * @throws UnknownKeyException
     *             If there is no such key in the schema.
     * @throws MissingParameterException
     *             If the parameter is required and was not set.
     * @throws ParameterTypeException
     *             If the class of the parameter as specified by the schema does not
     *             match this method's return type.
     */
    public Short getShort(String key) throws UnknownKeyException, MissingParameterException,
            ParameterTypeException {
        return get(key, SimpleSerializer.SHORT);
    }

    /**
     * Specifies that this object won't be used anymore (unlocks the schema). This
     * object may not be used after a call to this method, otherwise inconsistent
     * behaviour might occur.
     */
    public void dismiss() {
        schema.unlock(this);
    }

    /**
     * Returns a readable {@code String} representation of the value of the specified
     * parameter.
     * 
     * @param key
     *            The name of the parameter.
     * @return A {@code String} representing the value of the parameter.
     */
    private String valueToString(String key) {
        Serializer<?> serializer = getSpec(key).serializer;
        Object value = get(key);
        if (serializer instanceof SimpleSerializer) {
            return ((SimpleSerializer<?>) serializer).serialize(value);
        } else if (serializer instanceof ArraySerializer) {
            String[] array = ((ArraySerializer<?>) serializer).serialize((Object[]) value);
            StringBuilder sb = new StringBuilder("[");
            String separator = ", ";
            for (String s : array) {
                sb.append(s);
                sb.append(separator);
            }
            if (array.length > 0) {
                sb.delete(sb.length() - separator.length(), sb.length());
            }
            sb.append("]");
            return sb.toString();
        } else {
            return String.valueOf(value);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String key : schema.keys) {
            String value = valueToString(key);
            System.out.println(key + ": " + value);
        }
        return sb.toString();
    }
}
