package io.tracee.transport;

import io.tracee.TraceeConstants;
import io.tracee.transport.jaxb.TpicMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.soap.SOAPHeader;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import java.util.HashMap;
import java.util.Map;

public class SoapHeaderTransport {

	private static final SOAPHeaderMarshaller SOAP_HEADER_MARSHALLER = new SOAPHeaderMarshaller();
	private static final ResultMarshaller RESULT_MARSHALLER = new ResultMarshaller();
	private static final ElementUnmarshaller ELEMENT_UNMARSHALLER = new ElementUnmarshaller();
	private static final SourceUnmarshaller SOURCE_UNMARSHALLER = new SourceUnmarshaller();
	private final JAXBContext jaxbContext;
	private final Logger logger = LoggerFactory.getLogger(SoapHeaderTransport.class);


	public SoapHeaderTransport() {
		try {
			jaxbContext = JAXBContext.newInstance(TpicMap.class);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Retrieves the first TPIC header element out of the soap header and parse it
	 *
	 * @param soapHeader soap header of the message
	 * @return TPIC context map
	 */
	public Map<String, String> parseSoapHeader(final Element soapHeader) {
		final NodeList tpicHeaders = soapHeader.getElementsByTagNameNS(TraceeConstants.SOAP_HEADER_NAMESPACE, TraceeConstants.TPIC_HEADER);
		final HashMap<String, String> contextMap = new HashMap<String, String>();
		if (tpicHeaders != null && tpicHeaders.getLength() > 0) {
			for (int i = 0, items = tpicHeaders.getLength(); i < items; i++) {
				contextMap.putAll(parseTpicHeader((Element) tpicHeaders.item(i)));
			}
		}
		return contextMap;
	}

	/**
	 * Parses a context map from a given soap element.
	 */
	public Map<String, String> parseTpicHeader(final Element element) {
		return parseTpicHeader(ELEMENT_UNMARSHALLER, element);
	}

	public Map<String, String> parseTpicHeader(final Source source) {
		return parseTpicHeader(SOURCE_UNMARSHALLER, source);
	}

	private <T> Map<String, String> parseTpicHeader(final Unmarshallable<T> unmarshallable, final T xmlContext) {
		try {
			if (xmlContext != null) {
				final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
				final JAXBElement<TpicMap> unmarshal = unmarshallable.unmarshal(unmarshaller, xmlContext);
				if (unmarshal != null) {
					return unmarshal.getValue().unwrapValues();
				}
			}
		} catch (JAXBException e) {
			logger.warn("Unable to parse TPIC header: {}", e.getMessage());
			logger.debug("WithStack: Unable to parse TPIC header: {}", e.getMessage(), e);
		}
		return new HashMap<String, String>();
	}

	private interface Unmarshallable<T> {
		JAXBElement<TpicMap> unmarshal(Unmarshaller unmarshaller, T xmlContext) throws JAXBException;
	}

	private static class SourceUnmarshaller implements Unmarshallable<Source> {
		@Override
		public JAXBElement<TpicMap> unmarshal(final Unmarshaller unmarshaller, final Source source) throws JAXBException {
			return unmarshaller.unmarshal(source, TpicMap.class);
		}
	}

	private static class ElementUnmarshaller implements Unmarshallable<Element> {
		@Override
		public JAXBElement<TpicMap> unmarshal(final Unmarshaller unmarshaller, final Element element) throws JAXBException {
			return unmarshaller.unmarshal(element, TpicMap.class);
		}
	}

	/**
	 * Renders a given context map into a given soapHeader.
	 */
	public void renderSoapHeader(final Map<String, String> context, final SOAPHeader soapHeader) {
		renderSoapHeader(SOAP_HEADER_MARSHALLER, context, soapHeader);
	}

	/**
	 * Renders a given context map into a given result that should be the TPIC header node.
	 */
	public void renderSoapHeader(final Map<String, String> context, final Result result) {
		renderSoapHeader(RESULT_MARSHALLER, context, result);
	}

	/**
	 * Renders a given context map into a given result that should be the TPIC header node.
	 */
	private <T> void renderSoapHeader(final Marshallable<T> marshallable, final Map<String, String> context, T xmlContext) {
		if (context == null)
			return;
		try {
			final Marshaller marshaller = jaxbContext.createMarshaller();
			marshallable.marshal(marshaller, TpicMap.wrap(context), xmlContext);
		} catch (JAXBException e) {
			logger.warn("Unable to render TPIC header: {}", e.getMessage());
			logger.debug("WithStack: Unable to render TPIC header: {}", e.getMessage(), e);
		}
	}

	private interface Marshallable<T> {
		void marshal(Marshaller marshaller, TpicMap tpic, T xmlContext) throws JAXBException;
	}

	private static class ResultMarshaller implements Marshallable<Result> {
		@Override
		public void marshal(final Marshaller marshaller, final TpicMap tpic, final Result result) throws JAXBException {
			marshaller.marshal(tpic, result);
		}
	}

	private static class SOAPHeaderMarshaller implements Marshallable<SOAPHeader> {
		@Override
		public void marshal(final Marshaller marshaller, final TpicMap tpic, final SOAPHeader soapHeader) throws JAXBException {
			marshaller.marshal(tpic, soapHeader);
		}
	}
}
