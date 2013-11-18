package com.jbion.utils.xml.serialization.parameters;

import org.w3c.dom.DOMException;

import com.jbion.utils.xml.XmlHelper;
import com.jbion.utils.xml.serialization.serializers.ArraySerializer;
import com.jbion.utils.xml.serialization.serializers.Serializer;
import com.jbion.utils.xml.serialization.serializers.SimpleSerializer;

/**
 * Describes the specification for one particular parameter.
 * 
 * @author <a href="mailto:joffrey.bion@gmail.com">Joffrey Bion</a>
 */
class ParamSpec {

    @SuppressWarnings("serial")
    public static class InvalidKeyException extends RuntimeException {
        public InvalidKeyException(String s) {
            super(s);
        }
    }

    /**
     * The name of this parameter.
     */
    public final String key;
    /**
     * The serializer used for the class of this parameter.
     */
    public final Serializer<?> serializer;
    /**
     * Whether this parameter is required or not.
     */
    public final boolean required;
    /**
     * The default value for this parameter if it is not required.
     */
    public final Object defaultValue;
    /**
     * An optional description of this parameter.
     */
    public final String description;

    /**
     * Creates a required parameter specification, thus no default value is needed.
     * 
     * @param key
     *            The name of the parameter.
     * @param serializer
     *            The serializer handling the class of this parameter.
     */
    public <T> ParamSpec(String key, Serializer<T> serializer) {
        this(key, serializer, null);
    }

    /**
     * Creates a required parameter specification, thus no default value is needed.
     * 
     * @param key
     *            The name of the parameter.
     * @param serializer
     *            The serializer handling the class of this parameter.
     * @param description
     *            A description of this parameter.
     */
    public <T> ParamSpec(String key, Serializer<T> serializer, String description) {
        this(key, serializer, true, null, description);
    }

    /**
     * Creates a parameter specification.
     * 
     * @param key
     *            The name of the parameter.
     * @param serializer
     *            The serializer handling the class of this parameter.
     * @param required
     *            Whether this parameter is required.
     * @param defaultValue
     *            The default value if this parameter is optional.
     */
    public <T> ParamSpec(String key, Serializer<T> serializer, boolean required, T defaultValue) {
        this(key, serializer, required, defaultValue, null);
    }

    /**
     * Creates a parameter specification.
     * 
     * @param key
     *            The name of the parameter.
     * @param serializer
     *            The serializer handling the class of this parameter.
     * @param required
     *            Whether this parameter is required.
     * @param defaultValue
     *            The default value if this parameter is optional.
     * @param description
     *            A description of this parameter.
     */
    public <T> ParamSpec(String key, Serializer<T> serializer, boolean required, T defaultValue,
            String description) {
        if (key == null) {
            throw new IllegalArgumentException("The parameter's key cannot be null.");
        }
        if (serializer == null) {
            throw new IllegalArgumentException("The parameter's serializer cannot be null.");
        }
        checkKeyValidity(key);
        this.key = key;
        this.required = required;
        this.serializer = serializer;
        this.defaultValue = defaultValue;
        this.description = description;
    }

    /**
     * Throws a {@link InvalidKeyException} if the specified key is not a valid key.
     * 
     * @throws InvalidKeyException
     *             If the specified key is not a valid key.
     */
    private static void checkKeyValidity(String key) {
        try {
            XmlHelper.createEmptyDomDocument().createElement(key);
        } catch (DOMException e) {
            if (e.code == DOMException.INVALID_CHARACTER_ERR) {
                throw new InvalidKeyException("The specified key '" + key
                        + "' is not a valid XML name, thus not a valid parameter name.");
            }
            throw e;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof ParamSpec)) {
            return false;
        }
        ParamSpec ps = (ParamSpec) o;
        boolean equals = true;
        equals = equals && key.equals(ps.key);
        equals = equals && serializer.equals(ps.serializer);
        equals = equals && (required == ps.required);
        equals = equals && (defaultValue == ps.defaultValue);
        equals = equals && description.equals(ps.description);
        return equals;
    }

    /**
     * Returns a readable {@code String} representation of the default value of this
     * parameter specification.
     * 
     * @return A {@code String} representing the default value of the parameter.
     */
    private String defaultToString() {
        if (serializer instanceof SimpleSerializer) {
            return ((SimpleSerializer<?>) serializer).serialize(defaultValue);
        } else if (serializer instanceof ArraySerializer) {
            String[] array = ((ArraySerializer<?>) serializer).serialize((Object[]) defaultValue);
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
            return String.valueOf(defaultValue);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(key + " [");
        sb.append(serializer.getTypeName());
        sb.append("]");
        if (!required) {
            sb.append("(optional, default=" + defaultToString() + ")");
        }
        if (description != null) {
            sb.append("// " + description);
        }
        return sb.toString();
    }
}
