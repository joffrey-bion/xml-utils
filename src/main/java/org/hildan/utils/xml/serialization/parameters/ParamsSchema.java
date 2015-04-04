package org.hildan.utils.xml.serialization.parameters;

import java.util.HashMap;
import java.util.LinkedList;

import org.hildan.utils.xml.serialization.serializers.Serializer;

/**
 * Describes a schema of parameters needed by a program.
 * <p>
 * Each parameter has a unique name (a key) to be referred to, and can be required or optional. When
 * optional, a default value must be provided. One can also specify a short description for each
 * parameter.
 * </p>
 * <p>
 * Each parameter is associated to a {@link Serializer}, which also indicates its class.
 * </p>
 *
 * @author <a href="mailto:joffrey.bion@gmail.com">Joffrey Bion</a>
 */
public class ParamsSchema {

    @SuppressWarnings("serial")
    public static class LockedSchemaException extends RuntimeException {

        public LockedSchemaException() {
            super("Trying to modify a schema currently used by at least one Parameters object.");
        }
    }

    /**
     * The name of this schema, only used when serializing a {@link Parameters} object to an XML
     * file.
     */
    final String name;

    /**
     * The String indicating the version of the parameters that this schema describes.
     */
    private final int version;

    /**
     * A list of the keys in the order they were specified. Unlike specs.values()
     */
    LinkedList<String> keys;

    /**
     * A map between parameters' names and their {@link ParamSpec}s.
     */
    HashMap<String, ParamSpec> specs;

    /**
     * A list of all the {@link Parameters} objects that locked this schema.
     */
    private LinkedList<Parameters> lockers;

    /**
     * Whether this schema is locked.
     */
    private boolean locked;

    /**
     * Creates a new {@code ParamsSchema} with a default name.
     *
     * @param version
     *            The version of the parameters set that this schema describes. If several schemas
     *            are provided, this allows to select the schema to use when deserializing the XML
     *            file.
     */
    public ParamsSchema(int version) {
        this("parameters", version);
    }

    /**
     * Creates a new {@code ParamsSchema} with the specified name.
     *
     * @param name
     *            The name of this schema, only used when serializing a {@link Parameters} object to
     *            an XML file.
     * @param version
     *            The version of the parameters set that this schema describes. If several schemas
     *            are provided, this allows to select the schema to use when deserializing the XML
     *            file.
     */
    public ParamsSchema(String name, int version) {
        this.name = name;
        this.version = version;
        this.specs = new HashMap<>();
        this.keys = new LinkedList<>();
        this.locked = false;
        this.lockers = new LinkedList<>();
    }

    public int version() {
        return version;
    }

    /**
     * Locks this {@code ParamsSchema} to prevent further changes.
     *
     * @param p
     *            The caller of this method.
     */
    synchronized void lock(Parameters p) {
        locked = true;
        lockers.add(p);
    }

    /**
     * Unlocks this {@code ParamsSchema} to allow changes again.
     *
     * @param p
     *            The caller of this method.
     */
    synchronized void unlock(Parameters p) {
        lockers.remove(p);
        if (lockers.isEmpty()) {
            locked = false;
        }
    }

    /**
     * Throws a {@link LockedSchemaException} if this {@code ParamsSchema} is locked.
     *
     * @throws LockedSchemaException
     *             If this {@code ParamsSchema} is locked.
     */
    synchronized private void checkLock() {
        if (locked) {
            throw new LockedSchemaException();
        }
    }

    /**
     * Adds the given {@link ParamSpec} to this parameters schema.
     *
     * @param spec
     *            The specification to add.
     */
    synchronized private void add(ParamSpec spec) {
        checkLock();
        if (spec == null) {
            throw new IllegalArgumentException("A parameter specification cannot be null.");
        } else if (specs.containsKey(spec.key)) {
            throw new IllegalArgumentException("A parameter with this key already exists.");
        }
        specs.put(spec.key, spec);
        keys.add(spec.key);
    }

    /**
     * Adds a required parameter to the schema.
     *
     * @param key
     *            The name of the parameter.
     * @param serializer
     *            The serializer handling the class of this parameter.
     */
    public void addParam(String key, Serializer<?> serializer) {
        add(new ParamSpec(key, serializer, null));
    }

    /**
     * Adds a required parameter to the schema.
     *
     * @param key
     *            The name of the parameter.
     * @param serializer
     *            The serializer handling the class of this parameter.
     * @param description
     *            A description of this parameter.
     */
    public void addParam(String key, Serializer<?> serializer, String description) {
        add(new ParamSpec(key, serializer, true, null, description));
    }

    /**
     * Adds a parameter to the schema.
     *
     * @param key
     *            The name of the parameter.
     * @param serializer
     *            The serializer handling the class of this parameter.
     * @param defaultValue
     *            The default value if this parameter is optional.
     */
    public <T> void addOptionalParam(String key, Serializer<T> serializer, T defaultValue) {
        add(new ParamSpec(key, serializer, false, defaultValue, null));
    }

    /**
     * Adds a parameter to the schema.
     *
     * @param key
     *            The name of the parameter.
     * @param serializer
     *            The serializer handling the class of this parameter.
     * @param defaultValue
     *            The default value if this parameter is optional.
     * @param description
     *            A description of this parameter.
     */
    public <T> void addOptionalParam(String key, Serializer<T> serializer, T defaultValue, String description) {
        add(new ParamSpec(key, serializer, false, defaultValue, description));
    }

    /**
     * Adds all the specifications in the given {@link ParamsSchema} to this parameters schema.
     *
     * @param schema
     *            The schema to add.
     */
    synchronized public void addAll(ParamsSchema schema) {
        checkLock();
        if (schema == null) {
            throw new IllegalArgumentException("The specified schema cannot be null.");
        }
        for (String key : schema.specs.keySet()) {
            if (specs.containsKey(key)) {
                throw new IllegalArgumentException("This schema already contains the key '" + key + "'.");
            }
            add(schema.specs.get(key));
        }
    }

    /**
     * Returns {@code true} if all the specifications defined in the given {@link ParamsSchema}
     * exist in this parameters schema, and if they are all identical as defined by
     * {@link ParamSpec#equals(Object)}.
     *
     * @param otherSchema
     *            The whole schema to add.
     * @return <ul>
     *         <li>{@code true} if all the specifications defined in the given {@link ParamsSchema}
     *         exist in this parameters schema, and if they are all identical as defined by
     *         {@link ParamSpec#equals(Object)}.</li>
     *         <li>
     *         {@code false} if at least one {@link ParamSpec} of {@code otherSchema} is absent from
     *         this schema.</li>
     *         <li>
     *         {@code false} if at least one {@link ParamSpec} appears with the same key in this
     *         schema and {@code otherSchema} but is not the same as defined by
     *         {@link ParamSpec#equals(Object)}.</li>
     *         </ul>
     */
    public boolean isSuperSetOf(ParamsSchema otherSchema) {
        if (otherSchema == null) {
            throw new IllegalArgumentException("The specified schema cannot be null.");
        }
        for (String key : otherSchema.specs.keySet()) {
            if (!specs.containsKey(key)) {
                return false;
            }
            if (!otherSchema.specs.get(key).equals(specs.get(key))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Schema '");
        sb.append(name);
        sb.append("' (version ");
        sb.append(version);
        sb.append("):\n");
        for (String key : keys) {
            ParamSpec ps = specs.get(key);
            sb.append(ps.toString());
            sb.append("\n");
        }
        if (keys.size() > 0) {
            sb.delete(sb.length() - 1, sb.length());
        }
        return sb.toString();
    }
}
