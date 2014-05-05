package io.tracee.backend.log4j2;

import org.junit.Test;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class Log4j2TraceeBackendProviderTest {

	private final Log4j2TraceeBackendProvider unit = new Log4j2TraceeBackendProvider();

	@Test
	public void testProvideBackend() {
		assertThat(unit.provideBackend(), notNullValue());
	}

}
