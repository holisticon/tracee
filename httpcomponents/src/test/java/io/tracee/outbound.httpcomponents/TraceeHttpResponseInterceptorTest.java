package io.tracee.outbound.httpcomponents;

import io.tracee.SimpleTraceeBackend;
import io.tracee.TraceeConstants;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.HttpContext;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;

/**
 * @author Daniel Wegener (Holisticon AG)
 */
public class TraceeHttpResponseInterceptorTest {

	private final SimpleTraceeBackend backend = SimpleTraceeBackend.createNonLoggingAllPermittingBackend();
    private final TraceeHttpResponseInterceptor unit = new TraceeHttpResponseInterceptor(backend, null);

    @Test
    public void testResponseInterceptorParsesHttpHeaderToBackend() throws Exception {
        final HttpResponse httpResponse = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 404, "not found"));
        httpResponse.setHeader(TraceeConstants.HTTP_HEADER_NAME, "{\"foobi\":\"bar\"}");
        unit.process(httpResponse, mock(HttpContext.class));
        assertThat(backend.get("foobi"), equalTo("bar"));
    }


}
