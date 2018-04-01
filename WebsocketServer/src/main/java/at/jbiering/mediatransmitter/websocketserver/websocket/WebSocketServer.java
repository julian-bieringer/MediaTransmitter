package at.jbiering.mediatransmitter.websocketserver.websocket;

import java.io.StringReader;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.Logger;

import at.jbiering.mediatransmitter.websocketserver.model.Device;

@ApplicationScoped
@ServerEndpoint("/websocket")
public class WebSocketServer {
	
	@Inject
	private Logger logger;
	@Inject
	private SessionHandler sessionHandler;
	
	@OnOpen
	public void open(Session session) {
		logger.info("Websocket connection opened: " + session.toString());
		sessionHandler.addSession(session);
	}
	
	@OnClose
	public void close(Session session) {
		logger.info("Websocket connection closed: " + session.toString());
		sessionHandler.removeSession(session);
	}
	
	@OnError
	public void onError(Throwable error) {
		logger.warn("Error with websocket connection: " + error.getMessage());
	}	
	
	@OnMessage
	public void handleMessage(String message, Session session) {
		logger.info("Got incoming message from device " + session.getId() + ": " + message);
		
		try (JsonReader reader = Json.createReader(new StringReader(message))){
			JsonObject jsonMessage = reader.readObject();
			String action = jsonMessage.getString("action");
			
			
			if("add".equals(action)) {
				Device device = createDeviceFromJsonObject(jsonMessage);
				sessionHandler.addDevice(device);
			} else if("remove".equals(action)) {
				int id = (int) jsonMessage.getInt("id");
				sessionHandler.removeDevice(id);
			} else if ("toggle".equals(action)) {
				int id = (int) jsonMessage.getInt("id");
				sessionHandler.toggleDevice(id);
			}
		}
	}
	
	private Device createDeviceFromJsonObject(JsonObject msg) {
		String name = msg.getString("name");
		String description = msg.getString("description");
		String type = msg.getString("type");
		String status = msg.getString("status");
		return new Device(name, description, type, status);
	}
}