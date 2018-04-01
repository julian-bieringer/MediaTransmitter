package at.jbiering.mediatransmitter.websocketserver.websocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.spi.JsonProvider;
import javax.websocket.Session;

import org.slf4j.Logger;

import at.jbiering.mediatransmitter.websocketserver.model.Device;

@ApplicationScoped
public class SessionHandler {

	@Inject
	private Logger logger;
	
	private final Set<Session> sessions = new HashSet<>();
	private final Set<Device> devices = new HashSet<>();
	private long deviceId = 1;
	
	public void addSession(Session session) {
		sessions.add(session);
		
		for(Device device : devices) {
			JsonObject addMessage = createAddMessage(device);
			sendToSession(session, addMessage);
		}
	}
	
	public void removeSession(Session session) {
		sessions.remove(session);
	}
	
	public List<Device> getDevices(){
		return new ArrayList<>(devices);
	}
	
	public void addDevice(Device device) {
		device.setId(deviceId);
		deviceId++;
		JsonObject addMessage = createAddMessage(device);
		sendToAllConnectedSessions(addMessage);
		this.devices.add(device);
	}
	
	public void removeDevice(int id) {
		Device device = findDeviceById(id);
		if(device != null) {
			devices.remove(device);
			
			JsonProvider provider = JsonProvider.provider();
			JsonObject removeMessage = provider.createObjectBuilder()
					.add("action", "remove")
					.add("id", id)
					.build();
			sendToAllConnectedSessions(removeMessage);
		}
	}
	
	public void toggleDevice(int id) {
		JsonProvider provider = JsonProvider.provider();
		Device device = findDeviceById(id);
		
		if(device != null) {
			if(device.getStatus().toLowerCase().equals("on")) {
				device.setStatus("off");
			} else if (device.getStatus().toLowerCase().equals("off")) {
				device.setStatus("on");
			}
			
			JsonObject updateMesssage = provider.createObjectBuilder()
					.add("action", "toggle")
					.add("id", device.getId())
					.add("status", device.getStatus())
					.build();
			sendToAllConnectedSessions(updateMesssage);
		}
    }

    private JsonObject createAddMessage(Device device) {
        JsonProvider provider = JsonProvider.provider();
        JsonObject addMessage = provider.createObjectBuilder()
        		.add("action", "add")
                .add("id", device.getId())
                .add("name", device.getName())
                .add("type", device.getType())
                .add("status", device.getStatus())
                .add("description", device.getDescription())
                .build();
        return addMessage;
    }

    private void sendToAllConnectedSessions(JsonObject message) {
    	for(Session session : sessions)
    		sendToSession(session, message);
    }

    private void sendToSession(Session session, JsonObject message) {
    	try {
			session.getBasicRemote().sendText(message.toString());
		} catch (IOException e) {
			sessions.remove(session);
			logger.error("Sending message to session #" + session.getId() + " failed with message:" + message);
		}
    }
	
	
	private Device findDeviceById(int id) {
		for(Device device : devices) {
			if(device.getId() == id)
				return device;
		}
		return null;
	}
	
}
