/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2012 Johannes Schnatterer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package info.schnatterer.test.shutdown;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

/**
 * This provides the logic for gracefully terminating JUnit test by entering a
 * specific signal string into the console. Before terminating the process a
 * user defined shutdown hook is run, which allows for stopping servers,
 * database cleanup, closing resources, etc.
 * 
 * @author schnatterer
 * 
 */
public class JUnitShutdown {
	/** Log4j logger. */
	private Logger log = Logger.getLogger(this.getClass());

	/** User defined shutdown hook thread. */
	private Thread shutdownHookThread;
	/**
	 * The "signal" string, whose input on the console initiates the shutdown of
	 * the test.
	 */
	private String exitSignalString;
	/** Listen for the signal only if the test is run in debug mode. */
	private boolean isDebugOnly;

	/**
	 * Creates an instance of the shutdownhook that listens to the console for
	 * an <code>exitSignal</code> and executes <code>shutdownHook</code> when
	 * the signal is received.
	 * 
	 * @param exitSignal
	 *            the signal the leads to exiting the test.
	 * @param isUsedInDebugOnly
	 *            if <code>true</code>, <code>exitSignal</code> is only
	 *            evaluated if test is run in debug mode
	 * @param shutdownHook
	 *            the thread that is executed when <code>exitSignal</code> is
	 *            received.
	 */
	public JUnitShutdown(final String exitSignal,
			final boolean isUsedInDebugOnly, final Thread shutdownHook) {
		shutdownHookThread = shutdownHook;
		exitSignalString = exitSignal;
		this.isDebugOnly = isUsedInDebugOnly;
		initShutdownHook();
	}

	/**
	 * Allows for cleanup before test cancellation by listening to the console
	 * for a specific exitSignal. On this signal registers a shutdown hook who
	 * performs the cleanup in separate thread.
	 */
	private void initShutdownHook() {
		if (isDebugOnly
				&& java.lang.management.ManagementFactory.getRuntimeMXBean()
						.getInputArguments().toString()
						.contains("-agentlib:jdwp")) {
			return;
		}

		/* Start thread which listens to system.in */
		Thread consoleListener = new Thread() {
			@Override
			public void run() {
				BufferedReader bufferReader = null;
				try {
					bufferReader = new BufferedReader(new InputStreamReader(
							System.in));
					/* Read from system.in */
					while (!bufferReader.readLine().equals(exitSignalString)) {
						doNothing();
					}

					// Add shutdown hook that performs cleanup
					Runtime.getRuntime().addShutdownHook(shutdownHookThread);

					log.debug("Received exit signal \"" + exitSignalString
							+ "\". Shutting down test.");
					System.exit(0);
				} catch (IOException e) {
					log.debug("Error reading from console", e);
				}
			}

			/**
			 * Is not doing a thing.
			 */
			private void doNothing() {
			}
		};
		consoleListener.start();
	}

}
