package iot.challenge.jura.graba.web.websocket;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;

import iot.challenge.jura.util.trait.Loggable;

/**
 * Class to be implemented by websockets.
 * 
 * WARNING!. Don't (!) extend WebSocketServlet directly. Use this class (check
 * the comments to know why)
 */
public abstract class DelayedWebSocketServlet extends WebSocketServlet implements Loggable {

	private static final long serialVersionUID = -107948209393999604L;

	protected final AtomicBoolean firstCall = new AtomicBoolean(true);
	protected final CountDownLatch initBarrier = new CountDownLatch(1);

	@Override
	public void init() throws ServletException {
		// FIXME https://issues.apache.org/jira/browse/FELIX-5310
		//
		// The servlet has been initialized, but we delay initialization until the first
		// request so that a Jetty Context is available
	}

	@Override
	public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
		if (firstCall.compareAndSet(true, false)) {
			try {
				delayedInit();
			} finally {
				initBarrier.countDown();
			}
		} else {
			try {
				initBarrier.await();
			} catch (InterruptedException e) {
				throw new ServletException("Timed out waiting for initialisation", e);
			}
		}

		super.service(request, response);
	}

	protected void delayedInit() throws ServletException {
		// Overide the TCCL so that the internal factory can be found. Jetty tries to
		// use ServiceLoader, and their fallback is to use TCCL, it would be better if
		// we could provide a loader...
		Thread currentThread = Thread.currentThread();
		ClassLoader tccl = currentThread.getContextClassLoader();
		currentThread.setContextClassLoader(WebSocketServlet.class.getClassLoader());
		try {
			super.init();
		} finally {
			currentThread.setContextClassLoader(tccl);
		}
	}
}