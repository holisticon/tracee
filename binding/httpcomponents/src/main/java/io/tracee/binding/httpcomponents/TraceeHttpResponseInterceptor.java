package io.tracee.binding.httpcomponents;

import io.tracee.Tracee;
import io.tracee.TraceeBackend;
import io.tracee.TraceeConstants;
import io.tracee.configuration.TraceeFilterConfiguration;
import io.tracee.transport.HttpHeaderTransport;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static io.tracee.configuration.TraceeFilterConfiguration.Channel.IncomingResponse;


public class TraceeHttpResponseInterceptor implements HttpResponseInterceptor {

    private final TraceeBackend backend;
    private final HttpHeaderTransport transportSerialization;
	private final String profile;

	public TraceeHttpResponseInterceptor() {
		this(null);
	}

	public TraceeHttpResponseInterceptor(String profile) {
		this(Tracee.getBackend(), profile);
	}

	TraceeHttpResponseInterceptor(TraceeBackend backend, String profile) {
		this.backend = backend;
		this.profile = profile;
		transportSerialization = new HttpHeaderTransport(backend.getLoggerFactory());
	}

	@Override
    public final void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
        final TraceeFilterConfiguration filterConfiguration = backend.getConfiguration(profile);
		final Header[] responseHeaders = response.getHeaders(TraceeConstants.HTTP_HEADER_NAME);
        if (responseHeaders != null && responseHeaders.length > 0 && filterConfiguration.shouldProcessContext(IncomingResponse)) {
			final List<String> stringTraceeHeaders = new ArrayList<String>();
			for (Header header : responseHeaders) {
				stringTraceeHeaders.add(header.getValue());
			}
			backend.putAll(filterConfiguration.filterDeniedParams(transportSerialization.parse(stringTraceeHeaders), IncomingResponse));
		}
    }
}
