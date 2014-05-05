package io.tracee.examples.jaxws.client;


import io.tracee.Tracee;
import io.tracee.TraceeBackend;
import io.tracee.TraceeConstants;
import io.tracee.TraceeLogger;
import io.tracee.examples.jaxws.client.testclient.TraceeJaxWsTestService;
import io.tracee.examples.jaxws.client.testclient.TraceeJaxWsTestWS;
import io.tracee.jaxws.client.TraceeClientHandlerResolver;
import org.slf4j.MDC;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Tobias Gindler (Holisticon AG)
 */
public final class TestClient {

    private TestClient() {

    }

    public static void main(final String[] args) throws MalformedURLException {

        final TraceeBackend traceeBackend = Tracee.getBackend();
        final TraceeLogger traceeLogger = traceeBackend.getLoggerFactory().getLogger(TestClient.class);

        final TraceeJaxWsTestService testWebservice = new TraceeJaxWsTestService(
                new URL("http://localhost:8081/traceeJaxwsTestService/webservices/TraceeJaxWsTestService?wsdl"));
        testWebservice.setHandlerResolver(new TraceeClientHandlerResolver());


        final TraceeJaxWsTestWS ws = testWebservice.getPort(TraceeJaxWsTestWS.class);

        final int a = 2;
        final int b = 3;

        traceeBackend.remove(TraceeConstants.REQUEST_ID_KEY);
        MDC.remove(TraceeConstants.REQUEST_ID_KEY);


        traceeLogger.info("WS CALL WITH NONEXISTING REQUEST_ID : " + a + "+" + b + "=" + ws.sum(a, b));


        traceeBackend.put(TraceeConstants.REQUEST_ID_KEY, "XYX");
        traceeLogger.info("WS CALL WITH EXISTING REQUEST_ID : " + a + "+" + b + "=" + ws.sum(a, b));
        traceeBackend.remove(TraceeConstants.REQUEST_ID_KEY);
        MDC.remove(TraceeConstants.REQUEST_ID_KEY);



/*
        try {
            ws.error(a,b);
        } catch (Exception e) {
            traceeLogger.error("GOT EXPECTED EXCEPTION");
        }
*/

    }

}
