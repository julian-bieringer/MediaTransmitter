package at.jbiering.mediatransmitter.websocketserver.websocket;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.json.JsonObject;
import javax.json.spi.JsonProvider;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.WebSocketContainer;

import org.junit.Test;

import at.jbiering.mediatransmitter.websocketserver.websocket.model.Device;
import at.jbiering.mediatransmitter.websocketserver.websocket.model.enums.Action;


public class WebsocketServerTests {
	
	private static final String uri = "ws://localhost:8080/websocket";
	static CountDownLatch messageLatch;

	@Test
	public void websocketConnection_buildUpWebsocketConnection_connectionEstablished() throws DeploymentException, IOException, InterruptedException {
		WebsocketServerTests.messageLatch = new CountDownLatch(1);
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        DeviceClientEndpoint client = new DeviceClientEndpoint("jbiering");
        container.connectToServer(client, URI.create(uri));
		WebsocketServerTests.messageLatch.await(5, TimeUnit.SECONDS);
        assertEquals(0, WebsocketServerTests.messageLatch.getCount());
        client.sendMessage(createCloseMessage(client.getCurrentDevice().getId()));
        client.closeConnection();
	}
	
	@Test
	public void websocketSubscribers_retrieveWebsocketSubscribers_getEmptySubscribersArray() throws DeploymentException, IOException, InterruptedException {
		WebsocketServerTests.messageLatch = new CountDownLatch(1);
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        DeviceClientEndpoint client = new DeviceClientEndpoint("jbiering");
        container.connectToServer(client, URI.create(uri));
        WebsocketServerTests.messageLatch.await(5, TimeUnit.SECONDS);
        assertEquals(0, WebsocketServerTests.messageLatch.getCount());
		
		WebsocketServerTests.messageLatch = new CountDownLatch(1);
    	JsonProvider provider = JsonProvider.provider();
    	JsonObject msg = provider.createObjectBuilder()
    			.add("action", Action.RETRIEVE_SUBSCRIBERS.toString())
    			.add("id", client.getCurrentDevice().getId())
    			.build();
		
    	client.sendMessage(msg);
    	WebsocketServerTests.messageLatch.await(5, TimeUnit.SECONDS);
        assertEquals(0, WebsocketServerTests.messageLatch.getCount());
		
		Device[] devices = client.getDevices();
		
		assertEquals(0, devices.length);
        client.sendMessage(createCloseMessage(client.getCurrentDevice().getId()));
        client.closeConnection();
	}
	
	@Test
	public void websocketSubscribers_retrieveWebsocketSubscribers_getOneSubscribersArray() throws DeploymentException, IOException, InterruptedException {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
       
        //Client 1 connects to server
        //CountDownLatch is 1 for the incoming message with action "add"
		WebsocketServerTests.messageLatch = new CountDownLatch(1);
        DeviceClientEndpoint client1 = new DeviceClientEndpoint("jbiering");
        container.connectToServer(client1, URI.create(uri));
        WebsocketServerTests.messageLatch.await(5, TimeUnit.SECONDS);
        assertEquals(0, WebsocketServerTests.messageLatch.getCount());
        
        //Client 2 connects to server
        //CountDownLatch is 2 for the incoming message with action "add" for client 2
        //and incoming message "subscriber_list_update_required" for client 1
		WebsocketServerTests.messageLatch = new CountDownLatch(2);
        DeviceClientEndpoint client2 = new DeviceClientEndpoint("mspencer");
        container.connectToServer(client2, URI.create(uri));
        WebsocketServerTests.messageLatch.await(5, TimeUnit.SECONDS);
        assertEquals(0, WebsocketServerTests.messageLatch.getCount());
		
		WebsocketServerTests.messageLatch = new CountDownLatch(1);
    	JsonProvider provider = JsonProvider.provider();
    	JsonObject msg = provider.createObjectBuilder()
    			.add("action", Action.RETRIEVE_SUBSCRIBERS.toString())
    			.add("id", client1.getCurrentDevice().getId())
    			.build();
		
    	client1.sendMessage(msg);
    	WebsocketServerTests.messageLatch.await(5, TimeUnit.SECONDS);
        assertEquals(0, WebsocketServerTests.messageLatch.getCount());
		
		Device[] devices = client1.getDevices();
		
		assertEquals(1, devices.length);
		
		//Client 1 disconnects
		//CountDownLatch is 1 as client 2 should receive and "subscriber_list_update_required" message
        WebsocketServerTests.messageLatch = new CountDownLatch(1);
        client1.sendMessage(createCloseMessage(client1.getCurrentDevice().getId()));
        client1.closeConnection();
        
    	WebsocketServerTests.messageLatch.await(5, TimeUnit.SECONDS);
        assertEquals(0, WebsocketServerTests.messageLatch.getCount());
        
        WebsocketServerTests.messageLatch = new CountDownLatch(1);
        msg = provider.createObjectBuilder()
    			.add("action", Action.RETRIEVE_SUBSCRIBERS.toString())
    			.add("id", client2.getCurrentDevice().getId())
    			.build();
        client2.sendMessage(msg);
        
        WebsocketServerTests.messageLatch.await(5, TimeUnit.SECONDS);
        assertEquals(0, WebsocketServerTests.messageLatch.getCount());
		
		devices = client2.getDevices();
		
		assertEquals(0, devices.length);
        
        //Client 2 disconnects
        client2.sendMessage(createCloseMessage(client2.getCurrentDevice().getId()));
        client2.closeConnection();
	}
	
	private JsonObject createCloseMessage(long id) {
		JsonProvider provider = JsonProvider.provider();
    	JsonObject msg = provider.createObjectBuilder()
    			.add("action", Action.REMOVE.toString())
    			.add("id", id)
    			.build();
    	return msg;
	}
}