package info.schnatterer.test.sometest;

import info.schnatterer.test.shutdown.JUnitShutdown;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Test;

/**
 * An exemplary test that runs long (like forever) but can be shutdown by
 * entering "q" on the console.
 * 
 * @author schnatterer
 * 
 */
public class SomeLongRunningTest {
	/** Log4j logger. */
	private Logger log = Logger.getLogger(this.getClass());
	/**
	 * An exemplary resource which not deleted when the test gets terminated,
	 * but gets deleted, when using {@link JUnitShutdown}.
	 */
	private static final String SOME_FILE = "some.file";
	/**
	 * An exemplary resource which is not closed when the test gets terminated,
	 * but gets closed, when using {@link JUnitShutdown}.
	 */
	private BufferedWriter someResource = null;

	/** A value that controls how many values are output during the test. */
	private static final int SOME_DIVISOR = 100000000;

	/**
	 * The shutdown hook listening to the exit signal.
	 */
	@SuppressWarnings("unused")
	private JUnitShutdown shutdownHook = new JUnitShutdown("q", false,
			new Thread("testShutdownHook") {
				public void run() {
					cleanupResources("shutdown hook");
				}
			});

	/**
	 * Some exemplary test.
	 * 
	 * @throws IOException
	 *             some error
	 */
	@Test
	public void testSomething() throws IOException {
		try {
			someResource = new BufferedWriter(new FileWriter(
					new File(SOME_FILE), false));

			doSomethingExpensive();

		} finally {
			cleanupResources("testSomething()");
		}
	}

	/**
	 * Keeps the machine busy forever.
	 * 
	 * @throws IOException
	 *             something went wrong during writing to file
	 */
	private void doSomethingExpensive() throws IOException {
		int i = 0;
		while (true) {
			if (++i % SOME_DIVISOR == 0) {
				log.debug(i);
				someResource.write(i);
				someResource.newLine();
			}
		}
	}

	/**
	 * This method is only called when the test ends without being killed.
	 */
	@After
	public void onTearDown() {
		cleanupResources("onTearDown()");
	}

	/**
	 * Closes {@link #someResource} and deletes {@link #SOME_FILE}.
	 * 
	 * @param caller
	 *            the caller of this method for logging purpose only.
	 */
	private void cleanupResources(final String caller) {
		log.debug("cleanupResources() called by " + caller);
		if (someResource != null) {
			try {
				someResource.close();
			} catch (IOException e) {
				log.error("Unable to close resource", e);
			}
		}
		File f = new File(SOME_FILE);
		if (f.exists() && !f.isDirectory()) {
			f.delete();
		}
	}
}
