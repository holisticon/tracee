package io.tracee.binding.jaxws;


import io.tracee.*;
import io.tracee.transport.SoapHeaderTransport;

import javax.xml.soap.*;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.util.Map;

import static io.tracee.configuration.TraceeFilterConfiguration.Channel.IncomingRequest;
import static io.tracee.configuration.TraceeFilterConfiguration.Channel.OutgoingResponse;

public class TraceeServerHandler extends AbstractTraceeHandler {

	private final TraceeLogger traceeLogger;

	private final SoapHeaderTransport transportSerialization;

	public TraceeServerHandler() {
		this(Tracee.getBackend(), new SoapHeaderTransport());
	}

	public TraceeServerHandler(TraceeBackend traceeBackend, SoapHeaderTransport soapHeaderTransport) {
		super(traceeBackend);
		this.transportSerialization = soapHeaderTransport;
		traceeLogger = traceeBackend.getLoggerFactory().getLogger(TraceeServerHandler.class);
	}

	protected final void handleIncoming(SOAPMessageContext context) {
		final SOAPMessage soapMessage = context.getMessage();
		try {
			final SOAPHeader header = soapMessage.getSOAPHeader();

			if (header != null && traceeBackend.getConfiguration().shouldProcessContext(IncomingRequest)) {
				final Map<String, String> parsedContext = transportSerialization.parseSoapHeader(header);
				final Map<String, String> filteredContext = traceeBackend.getConfiguration().filterDeniedParams(parsedContext, IncomingRequest);
				traceeBackend.putAll(filteredContext);
			}
		} catch (final Exception e) {
			traceeLogger.error("TraceeServerHandler - Error during precessing of inbound soap header");
			traceeLogger.debug("TraceeServerHandler - Error during precessing of inbound soap header", e);
		}

		Utilities.generateRequestIdIfNecessary(traceeBackend);
		Utilities.generateConversationIdIfNecessary(traceeBackend);
	}

	protected final void handleOutgoing(SOAPMessageContext context) {
		final SOAPMessage msg = context.getMessage();
		try {
			if (msg != null && !traceeBackend.isEmpty() && traceeBackend.getConfiguration().shouldProcessContext(OutgoingResponse)) {

				// get or create header
				final SOAPHeader header = getOrCreateHeader(msg);

				final Map<String, String> filteredContext = traceeBackend.getConfiguration().filterDeniedParams(traceeBackend.copyToMap(), OutgoingResponse);
				transportSerialization.renderSoapHeader(filteredContext, header);

				msg.saveChanges();
				context.setMessage(msg);
			}

		} catch (final Exception e) {
			traceeLogger.error("TraceeServerHandler : Exception occurred during processing of outbound message.");
			traceeLogger.debug("TraceeServerHandler : Exception occurred during processing of outbound message.", e);
		} finally {
			// must reset tracee context
			traceeBackend.clear();
		}
	}

	SOAPHeader getOrCreateHeader(final SOAPMessage message) throws SOAPException {
		SOAPHeader header = message.getSOAPHeader();
		if (header == null) {
			header =  message.getSOAPPart().getEnvelope().addHeader();
		}
		return header;
	}

	@Override
	public boolean handleFault(SOAPMessageContext context) {
		this.handleOutgoing(context);
		return true;
	}
}
