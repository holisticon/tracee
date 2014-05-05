package io.tracee.outbound.httpcomponents;

import io.tracee.Tracee;
import io.tracee.TraceeBackend;
import io.tracee.TraceeConstants;
import io.tracee.configuration.TraceeFilterConfiguration;
import io.tracee.transport.HttpJsonHeaderTransport;
import io.tracee.transport.TransportSerialization;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.util.Map;

import static io.tracee.configuration.TraceeFilterConfiguration.Channel.IncomingResponse;

/**
 * @author Daniel Wegener (Holisticon AG)
 */
public class TraceeHttpResponseInterceptor implements HttpResponseInterceptor {


    private final TraceeBackend backend;
    private final TransportSerialization<String> transportSerialization;
	private final String profile;

	TraceeHttpResponseInterceptor(TraceeBackend backend, String profile) {
		this.backend = backend;
		this.profile = profile;
		transportSerialization = new HttpJsonHeaderTransport(backend.getLoggerFactory());
	}

	public TraceeHttpResponseInterceptor() {
		this(null);
	}
	public TraceeHttpResponseInterceptor(String profile) {
		this(Tracee.getBackend(), profile);
	}

	@Override
    public final void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
        final TraceeFilterConfiguration filterConfiguration = backend.getConfiguration(profile);
		final Header traceeHeader = response.getFirstHeader(TraceeConstants.HTTP_HEADER_NAME);
        if (traceeHeader != null && filterConfiguration.shouldProcessContext(IncomingResponse)) {
			final Map<String, String> parsedContext = transportSerialization.parse(traceeHeader.getValue());
			backend.putAll(filterConfiguration.filterDeniedParams(parsedContext, IncomingResponse));
		}
    }
}
