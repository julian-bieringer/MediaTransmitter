package at.jbiering.mediatransmitter.websocketserver.websocket;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

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
import at.jbiering.mediatransmitter.websocketserver.websocket.enums.Action;

@ApplicationScoped
@ServerEndpoint("/websocket")
public class WebSocketServer {
	
	@Inject
	private Logger logger;
	@Inject
	private SessionHandler sessionHandler;
	
	@OnOpen
	public void open(Session session) {
		logger.info("*** Websocket connection opened ***");
		sessionHandler.addSession(session);
	}
	
	@OnClose
	public void close(Session session) {
		logger.info("*** Websocket connection closed ***");
		sessionHandler.removeSession(session);
	}
	
	@OnError
	public void onError(Throwable error) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		error.printStackTrace(pw);
		String stackTraceString = sw.toString();
		
		logger.warn("*** Error with websocket connection ***: " + stackTraceString);
	}	
	
	@OnMessage
	public void handleMessage(String message, Session session) {
		logger.info("*** Got incoming message from device ***: " + message);
		
		try (JsonReader reader = Json.createReader(new StringReader(message))){
			JsonObject jsonMessage = reader.readObject();
			String actionString = jsonMessage.getString("action");
			Action action = Enum.valueOf(Action.class, actionString.toUpperCase());
			
			logger.info("*** Chosen action is ***: " + action.toString());
			
			if(Action.ADD.equals(action)) {
				Device device = createDeviceFromJsonObject(jsonMessage);
				sessionHandler.addDevice(device, session);
			} else if(Action.REMOVE.equals(action)) {
				int id = (int) jsonMessage.getInt("id");
				sessionHandler.removeDevice(id);
			} else if (Action.TOGGLE.equals(action)) {
				int id = (int) jsonMessage.getInt("id");
				sessionHandler.toggleDevice(id);
			} else if (Action.RETRIEVE_SUBSCRIBERS.equals(action)) {
				int id = (int) jsonMessage.getInt("id");
				sessionHandler.sendSubscribersList(session, id);
			}
		}
	}
	
	private Device createDeviceFromJsonObject(JsonObject msg) {
		String name = msg.getString("name");
		String modelDescription = msg.getString("modelDescription");
		String type = msg.getString("type");
		String status = "on";
		String osType = msg.getString("osType");
		String osVersion = msg.getString("osVersion");
		return new Device(name, status, type, modelDescription, osType, osVersion);
	}
}