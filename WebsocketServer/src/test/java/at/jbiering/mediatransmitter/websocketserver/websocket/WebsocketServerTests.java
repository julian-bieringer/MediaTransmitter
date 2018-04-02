package at.jbiering.mediatransmitter.websocketserver.websocket;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.WebSocketContainer;

import org.junit.Test;


public class WebsocketServerTests {
	
	private static final String uri = "ws://localhost:8080/websocket";
	final static CountDownLatch messageLatch = new CountDownLatch(1);

	@Test
	public void websocketConnection_buildUpWebsocketConnection_connectionEstablished() throws DeploymentException, IOException, InterruptedException {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.connectToServer(DeviceClientEndpoint.class, URI.create(uri));
		messageLatch.await(2, TimeUnit.SECONDS);
	}
}