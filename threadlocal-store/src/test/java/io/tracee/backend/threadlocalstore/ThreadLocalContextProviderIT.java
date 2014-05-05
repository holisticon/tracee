package io.tracee.backend.threadlocalstore;

import io.tracee.Tracee;
import io.tracee.TraceeBackend;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;


/**
 * @author Daniel
 */
public class ThreadLocalContextProviderIT {


    @Test
    public void testLoadProvierThenStoreAndRetrieve() {
        final TraceeBackend context = Tracee.getBackend();
        context.put("FOO", "BAR");
        assertThat(context.get("FOO"), equalTo("BAR"));
        context.clear();
    }


}
