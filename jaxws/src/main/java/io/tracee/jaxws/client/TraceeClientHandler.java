package io.tracee.jaxws.client;

import io.tracee.Tracee;
import io.tracee.TraceeBackend;
import io.tracee.TraceeLogger;
import io.tracee.jaxws.AbstractTraceeHandler;
import io.tracee.jaxws.protocol.SoapHeaderTransport;

import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.util.Map;

import static io.tracee.configuration.TraceeFilterConfiguration.Channel.IncomingResponse;
import static io.tracee.configuration.TraceeFilterConfiguration.Channel.OutgoingRequest;

public class TraceeClientHandler extends AbstractTraceeHandler {

    private final TraceeLogger traceeLogger = getTraceeBackend().getLoggerFactory().getLogger(TraceeClientHandler.class);

	private final SoapHeaderTransport transportSerialization = new SoapHeaderTransport();

	public TraceeClientHandler() {
		this(Tracee.getBackend());
	}

	TraceeClientHandler(TraceeBackend traceeBackend) {
		super(traceeBackend);
	}

    @Override
    public final boolean handleFault(final SOAPMessageContext context) {
        return true;
    }

    protected final void handleIncoming(final SOAPMessageContext context) {

        final SOAPMessage msg = context.getMessage();
		final TraceeBackend backend = getTraceeBackend();
        if (msg != null && backend.getConfiguration().shouldProcessContext(OutgoingRequest)) {

            try {
                final SOAPEnvelope env = msg.getSOAPPart().getEnvelope();

                // get soap header
                final SOAPHeader header = env.getHeader();

				if (header != null) {
					parseSoapHeaderToBackend(header);
				}

            } catch (final SOAPException e) {
                e.printStackTrace();
                traceeLogger.error(
                        "TraceeClientHandler : Exception occurred during processing of inbound message.", e);
            }

        }
    }

	final void parseSoapHeaderToBackend(SOAPHeader soapHeader) {
		final Map<String, String> parsedContext = transportSerialization.parse(soapHeader);
		final Map<String, String> filteredContext = getTraceeBackend().getConfiguration().filterDeniedParams(parsedContext, OutgoingRequest);
		getTraceeBackend().putAll(filteredContext);
	}


    protected final void handleOutgoing(final SOAPMessageContext context) {

        final SOAPMessage msg = context.getMessage();
        final TraceeBackend backend = getTraceeBackend();
		if (msg != null && backend.getConfiguration().shouldProcessContext(IncomingResponse)) {

            try {
                final SOAPEnvelope env = msg.getSOAPPart().getEnvelope();
                // get or create header
                SOAPHeader header = env.getHeader();
                if (header == null) {
                    header = env.addHeader();
                }

				final Map<String, String> filteredContext = backend.getConfiguration().filterDeniedParams(backend, IncomingResponse);
				transportSerialization.renderTo(filteredContext, header);

				msg.saveChanges();

            } catch (final SOAPException e) {
                traceeLogger.error("TraceeClientHandler : Exception "
                        + "occurred during processing of outbound message.", e);
            }

            context.setMessage(msg);

        }
    }



}
