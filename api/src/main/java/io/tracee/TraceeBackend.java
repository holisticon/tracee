package io.tracee;

import io.tracee.configuration.TraceeFilterConfiguration;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A backend is expected to be thread-safe (reads and writes are delegated to thread local state).
 */
public interface TraceeBackend {

	/**
	 * Gets the TraceeFilterConfiguration for a given profile.
	 */
	TraceeFilterConfiguration getConfiguration(String profileName);

	/**
	 * Gets the default TraceeFilterConfiguration.
	 */
	TraceeFilterConfiguration getConfiguration();

    /**
     * Gets a logger proxy that delegates to a concrete logging framework at runtime. Should only be used in tracee
     * adapters.
     */
    TraceeLoggerFactory getLoggerFactory();

    /**
     * @param key a non-null identifier.
     * @return {@code true} if this backend contains an entry for the given key. {@code false} otherwise.
     */
    boolean containsKey(String key);

    /**
     * Gets the value for a given key from this backend.
     * @param key a non-null identifier.
     * @return the stored value or {@code null} if not present.
     */
    String get(String key);

	int size();

	/**
	 * Clears all context information from this backend.
	 */
	void clear();

    /**
     * @return {@code true} if this backend contains no context information, {@code false} otherwise.
     */
    boolean isEmpty();

    /**
     * Puts a key into this backend.
     * @param key   ignored if {@code null}
     * @param value ignored if {@code null}
     */
    void put(String key, String value);

    void putAll(Map<? extends String, ? extends String> m);

	/**
	 * Generates a copy of the TraceeBackend. All keys known by TracEE are fetched from underlying MDC and copied to
	 * this map.
	 * @return immutable copy of the current state of the backend
	 */
	Map<String, String> copyToMap();

    /**
     * Removes the entry with the given key from this backend. Does nothing if the key is {@code null} or not found.
     * @param key ignored if {@code null}
     */
    void remove(String key);

	String getRequestId();
	String getSessionId();
	String getConversationId();
}
