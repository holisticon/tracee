package io.tracee;

import io.tracee.configuration.TraceeFilterConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;

public class MDCLikeTraceeBackendTest {

	private final MDCLike mdcLikeMock = Mockito.mock(MDCLike.class);
	@SuppressWarnings("unchecked")
	private final ThreadLocal<Set<String>> traceeKeysMock = (ThreadLocal<Set<String>>) Mockito.mock(ThreadLocal.class);
	@SuppressWarnings("unchecked")
	private final HashSet<String> traceeKeysSet = (HashSet<String>) Mockito.spy(new HashSet());

	private final MDCLikeTraceeBackend unit = new MDCLikeTraceeBackend(mdcLikeMock, traceeKeysMock, null);

	@Before
	public void setUpMocks() {
		when(traceeKeysMock.get()).thenReturn(traceeKeysSet);
	}

	@Test
	public void putWritesEntryToKeysSet() {
		unit.put("Foo", "bar");
		verify(traceeKeysSet).add("Foo");
	}

	@Test
	public void putWritesEntryToMdcLike() {
		unit.put("Foo", "bar");
		verify(mdcLikeMock).put("Foo", "bar");
	}

	@Test
	public void putAllWritesEntriesToKeysSet() {
		final Map<String, String> putMap = new HashMap<String, String>();
		putMap.put("Foo", "bar");
		putMap.put("Ping", "Pong");
		unit.putAll(putMap);
		verify(traceeKeysSet).add("Foo");
		verify(traceeKeysSet).add("Ping");
	}

	@Test
	public void putAllWritesEntriesToMdcLike() {
		final Map<String, String> putMap = new HashMap<String, String>();
		putMap.put("Foo", "bar");
		putMap.put("Ping", "Pong");
		unit.putAll(putMap);
		verify(mdcLikeMock).put("Foo", "bar");
		verify(mdcLikeMock).put("Ping", "Pong");
	}

	@Test
	public void clearAlsoRemovesTheThreadLocalTraceeKeys() {
		when(traceeKeysSet.iterator()).thenReturn(Collections.<String>emptyList().iterator());
		unit.clear();
		verify(traceeKeysMock).remove();
	}

	@Test
	public void clearRemovesRegisteredKeysFromMdcLike() {
		when(traceeKeysSet.iterator()).thenReturn(Arrays.asList("A", "B").iterator());
		unit.clear();
		verify(mdcLikeMock).remove("A");
		verify(mdcLikeMock).remove("B");
	}

	@Test
	public void removeRemovesRegisteredKeysFromMDC() {
		when(traceeKeysSet.remove("A")).thenReturn(true);
		when(traceeKeysSet.contains("A")).thenReturn(true);
		unit.remove("A");
		verify(mdcLikeMock).remove("A");
	}

	@Test
	public void removeDoesNotRemoveUnregisteredKeysFromMDC() {
		when(traceeKeysSet.remove("A")).thenReturn(false);
		when(traceeKeysSet.contains("A")).thenReturn(false);
		unit.remove("A");
		verify(mdcLikeMock, never()).remove("A");
	}

	@Test
	public void sizeCorrespondsToStoredKeysSize() {
		when(traceeKeysSet.size()).thenReturn(42);
		assertThat(unit.size(), equalTo(42));
	}

	@Test
	public void getValueFromMdcIfInKeySet() {
		when(traceeKeysSet.contains("A")).thenReturn(true);
		when(mdcLikeMock.get("A")).thenReturn("hurray");
		assertThat(unit.get("A"), equalTo("hurray"));
	}

	@Test
	public void getDoesNotReadDirectlyFromMdcLike() {
		when(traceeKeysSet.contains("A")).thenReturn(false);
		when(mdcLikeMock.get("A")).thenReturn("hurray");
		assertThat(unit.get("A"), nullValue());
	}

	@Test
	public void containsShouldReturnTrueIfInMDC() {
		when(traceeKeysSet.contains("A")).thenReturn(true);
		when(mdcLikeMock.containsKey("A")).thenReturn(true);
		assertThat(unit.containsKey("A"), is(true));
	}

	@Test
	public void containsShouldreturnFalseIfNotInMDC() {
		assertThat(unit.containsKey("A"), is(false));
	}

	@Test
	public void keysetShouldReturnCopyOfSet() {
		traceeKeysSet.addAll(Arrays.asList("A", "B", "C"));

		assertThat(unit.keySet(), containsInAnyOrder("A", "B", "C"));
		assertThat(unit.keySet(), hasSize(3));
	}

	@Test
	public void valuesShouldReturnOnlyTheValues() {
		traceeKeysSet.addAll(Arrays.asList("A", "B", "C"));
		when(mdcLikeMock.get("A")).thenReturn("vA");
		when(mdcLikeMock.get("B")).thenReturn("vB");
		when(mdcLikeMock.get("C")).thenReturn("vC");
		assertThat(unit.values(), containsInAnyOrder("vA", "vB" ,"vC"));
		assertThat(unit.values(), hasSize(3));
	}

	@Test
	public void entrySetShouldContainAllKeysAndValues() {
		traceeKeysSet.addAll(Arrays.asList("A", "B"));
		when(mdcLikeMock.get("A")).thenReturn("vA");
		when(mdcLikeMock.get("B")).thenReturn("vB");
		Set<Map.Entry<String, String>> entries = unit.entrySet();
		assertThat(entries, hasSize(2));
		for (Map.Entry<String, String> entry : entries) {
			assertThat(entry.getValue(), equalTo("v" + entry.getKey()));
		}
	}

	@Test
	public void shouldReturnFalseIfValueIsNotInMDC() {
		traceeKeysSet.addAll(Arrays.asList("A"));
		when(mdcLikeMock.get("A")).thenReturn("vA");
		assertThat(unit.containsValue("vB"), is(false));
	}

	@Test
	public void shouldReturnTrueIfKeyIsInMDC() {
		traceeKeysSet.addAll(Arrays.asList("A"));
		when(mdcLikeMock.get("A")).thenReturn("vA");
		assertThat(unit.containsValue("vA"), is(true));
	}

	@Test
	public void containsValueShouldHandleNullValuesInMDCMap() {
		traceeKeysSet.addAll(Arrays.asList("A"));
		when(mdcLikeMock.get("A")).thenReturn(null);
		assertThat(unit.containsValue("vA"), is(false));
	}

	@Test
	public void testLoadOverwrittenConfigurationValues() {
		assertThat(unit.getConfiguration().generatedRequestIdLength(), equalTo(42));
	}

	@Test
	public void testLoadUserDefinedProfileFromProperties() {
		assertThat(unit.getConfiguration("FOO").shouldProcessParam("ANY", TraceeFilterConfiguration.Channel.IncomingRequest), equalTo(true));
	}

    @Test
    public void testGeneratesRequestId() throws Exception {
        TraceeFilterConfiguration configuration = mock(TraceeFilterConfiguration.class);
        when(configuration.shouldGenerateRequestId()).thenReturn(true);

        unit.generateRequestIdIfNecessary(configuration);
        verify(mdcLikeMock, atLeastOnce()).put(eq(TraceeConstants.REQUEST_ID_KEY), anyString());
    }

    @Test
    public void testDoesNotGeneratesRequestId() throws Exception {
        TraceeFilterConfiguration configuration = mock(TraceeFilterConfiguration.class);
        when(configuration.shouldGenerateRequestId()).thenReturn(false);

        unit.generateRequestIdIfNecessary(configuration);
        verify(mdcLikeMock, never()).put(eq(TraceeConstants.REQUEST_ID_KEY), anyString());
    }

    @Test
    public void testGeneratesSessionId() throws Exception {
        TraceeFilterConfiguration configuration = mock(TraceeFilterConfiguration.class);
        when(configuration.shouldGenerateSessionId()).thenReturn(true);

        unit.generateSessionIdIfNecessary(configuration, "sessionId");
        verify(mdcLikeMock, atLeastOnce()).put(eq(TraceeConstants.SESSION_ID_KEY), anyString());
    }

    @Test
    public void testDoesNotGeneratesSessionId() throws Exception {
        TraceeFilterConfiguration configuration = mock(TraceeFilterConfiguration.class);
        when(configuration.shouldGenerateSessionId()).thenReturn(false);

        unit.generateSessionIdIfNecessary(configuration, "sessionId");
        verify(mdcLikeMock, never()).put(eq(TraceeConstants.SESSION_ID_KEY), anyString());
    }
}