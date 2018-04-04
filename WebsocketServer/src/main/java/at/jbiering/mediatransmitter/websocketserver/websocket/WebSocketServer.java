package at.jbiering.mediatransmitter.websocketserver.websocket;

import java.io.IOException;
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
import at.jbiering.mediatransmitter.websocketserver.model.MediaFile;
import at.jbiering.mediatransmitter.websocketserver.model.enums.Action;

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
		sessionHandler.removeSession(session);
		logger.info("*** Websocket connection closed ***");
	}
	
	@OnError
	public void onError(Throwable error) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		error.printStackTrace(pw);
		String stackTraceString = sw.toString();
		
		logger.warn("*** Error with websocket connection ***: " + stackTraceString);
		
		for(Device device : sessionHandler.getDevices())
			logger.info(" Device still in session handlers list: " + device.toString());
		
		for(Session session : sessionHandler.getSessions())
			logger.info(" Session still in session handlers list: " + session.getId());
	}	
	
	@OnMessage(maxMessageSize = 100 * 1024 * 1024)
	public void handleMessage(String message, Session session) {
		try (JsonReader reader = Json.createReader(new StringReader(message))){
			JsonObject jsonMessage = reader.readObject();
			String actionString = jsonMessage.getString("action");
			Action action = Enum.valueOf(Action.class, actionString.toUpperCase());
			
			logger.info("*** Got incoming message with action ***: " + action.toString());
			
			if(Action.ADD.equals(action)) {
				Device device = createDeviceFromJsonObject(jsonMessage);
				sessionHandler.addDevice(device, session);
			} else if(Action.REMOVE.equals(action)) {
				sessionHandler.removeDevice(session);
				session.close();
			} else if (Action.TOGGLE.equals(action)) {
				sessionHandler.toggleDevice(session);
			} else if (Action.RETRIEVE_SUBSCRIBERS.equals(action)) {
				sessionHandler.sendSubscribersList(session);
			} else if (Action.SEND_FILE.equals(action)) {
				long recipientId = jsonMessage.getInt("recipient_id");
				sessionHandler.sendByteArray(recipientId, retrieveMediaFile(jsonMessage));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
    private MediaFile retrieveMediaFile(JsonObject jsonMessage) {
		String bytesBase64 = jsonMessage.getString("bytes_base64");
		String fileName = jsonMessage.getString("file_name");
		String fileExtension = jsonMessage.getString("file_extension");
		return new MediaFile(bytesBase64, fileExtension, fileName);
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