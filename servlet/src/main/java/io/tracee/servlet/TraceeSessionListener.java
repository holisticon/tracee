package io.tracee.servlet;

import io.tracee.Tracee;
import io.tracee.TraceeBackend;
import io.tracee.TraceeConstants;
import io.tracee.Utilities;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * Warning: This filter assumes that the HttpSessionListener is executed on the same thread as the request handling.
 * This might not work for every servlet container.
 * It should at least work for the following containers:
 * <ul>
 * <li>Jetty</li>
 * <li>Tomcat</li>
 * </ul>
 *
 * @author Daniel Wegener (Holisticon AG)
 */
public class TraceeSessionListener implements HttpSessionListener {

	private final TraceeBackend backend;

	@SuppressWarnings("unused")
	public TraceeSessionListener() {
		this(Tracee.getBackend());
	}

	protected TraceeSessionListener(TraceeBackend backend) {
		this.backend = backend;
	}

    @Override
    public final void sessionCreated(HttpSessionEvent httpSessionEvent) {
		if (backend.getConfiguration().shouldGenerateSessionId()) {
			final String sessionId = httpSessionEvent.getSession().getId();
			backend.put(TraceeConstants.SESSION_ID_KEY, anonymizedSessionKey(sessionId, backend.getConfiguration().generatedSessionIdLength()));
		}
	}

    @Override
    public final void sessionDestroyed(HttpSessionEvent httpSessionEvent) {
		backend.remove(TraceeConstants.SESSION_ID_KEY);
    }

	private String anonymizedSessionKey(String sessionKey, int length) {
		return Utilities.createAlphanumericHash(sessionKey, length);
	}
}
