package io.tracee;

public final class TraceeConstants {

    private TraceeConstants() {
    }

    public static final String HTTP_HEADER_NAME = "X-TracEE-Context";
    public static final String JMS_HEADER_NAME = "X-TracEE-Context";

    public static final String SESSION_ID_KEY = "TracEE-sessionId";
    public static final String REQUEST_ID_KEY = "TracEE-requestId";


	public static final String PROFILE_HIDE_INBOUND = "HideInbound";
	public static final String PROFILE_HIDE_OUTBOUND = "HideOutbound";

}
