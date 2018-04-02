package at.jbiering.mediatransmitter.websocketserver.websocket;

import java.io.IOException;

import javax.json.JsonObject;
import javax.json.spi.JsonProvider;
import javax.websocket.ClientEndpoint;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

@ClientEndpoint
public class DeviceClientEndpoint {
	
	private Session session;
	
    @OnOpen
    public void onOpen(Session session) {
    	this.session = session;
        try {
        	String action = "add";
            String name = "jbiering";
            String type = "Medion";
            String modelDescription = "X5520";
            String osType = "Android";
            String osVersion = "7.0";
            
            JsonProvider provider = JsonProvider.provider();
            JsonObject message = provider.createObjectBuilder()
            		.add("action", action)
            		.add("name", name)
            		.add("type", type)
            		.add("modelDescription", modelDescription)
            		.add("osType", osType)
            		.add("osVersion", osVersion)
            		.build();
            
            session.getBasicRemote().sendText(message.toString());
        } catch (IOException ex) {
        }
    }

    @OnMessage
    public void processMessage(String message) {
        System.out.println("Received message in client: " + message);
        WebsocketServerTests.messageLatch.countDown();
        try {
			session.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    @OnError
    public void processError(Throwable t) {
        t.printStackTrace();
    }
}