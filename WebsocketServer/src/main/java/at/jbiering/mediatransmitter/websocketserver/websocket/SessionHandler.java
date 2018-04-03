package at.jbiering.mediatransmitter.websocketserver.websocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.spi.JsonProvider;
import javax.websocket.Session;

import org.slf4j.Logger;

import at.jbiering.mediatransmitter.websocketserver.model.Device;
import at.jbiering.mediatransmitter.websocketserver.websocket.enums.Action;

@ApplicationScoped
public class SessionHandler {

	@Inject
	private Logger logger;
	
	private final Set<Session> sessions = new HashSet<>();
	private final Set<Device> devices = new HashSet<>();
	private long deviceId = 1;
	
	public void addSession(Session session) {
		sessions.add(session);
	}
	
	public void removeSession(Session session) {
		sessions.remove(session);
	}
	
	public List<Device> getDevices(){
		return new ArrayList<>(devices);
	}
	
	public void addDevice(Device device, Session session) {
		//add new device and send only him his server information
		device.setId(deviceId);
		device.setSessionId(session.getId());
		deviceId++;
		JsonObject addMessage = createAddMessage(device);
		sendToSession(session, addMessage);
		
		//cannot use send to all connected sessions method because caller session
		//does not need to update his subscriber list
		
		JsonObject updateRequiredMessage = createSubscriberUpdateRequestedMessage();
		
		for(Session curr : sessions) {
			if(!(curr.getId().equals(session.getId()))) {
				sendToSession(curr, updateRequiredMessage);
			}
		}
		
		this.devices.add(device);
	}
	
	private JsonObject createSubscriberUpdateRequestedMessage() {
		JsonProvider provider = JsonProvider.provider();
		JsonObject object = provider.createObjectBuilder()
				.add("action", "subscriber_list_update_required")
				.build();
		return object;
	}

	public void removeDevice(int id) {
		Device device = findDeviceById(id);
		if(device != null) {
			devices.remove(device);
			Session session = findSessionById(device.getSessionId());
			
			if(session != null) {
				this.sessions.remove(session);
			}
			
			sendToAllConnectedSessions(createSubscriberUpdateRequestedMessage());
		}
	}
	
	private Session findSessionById(String sessionId) {
		for(Session session : sessions) {
			if(session.getId().equals(sessionId))
				return session;
		}
		return null;
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
    	JsonObject deviceMessage = createDeviceJsonObjectWithStatus(device, Action.ADD);
        return deviceMessage;
    }

    private void sendToAllConnectedSessions(JsonObject message) {
    	for(Session session : sessions)
    		sendToSession(session, message);
    }

    private void sendToSession(Session session, JsonObject message) {
    	try {
    		logger.info("Sending following message to client with session id [" + session.getId() + "]: " + message.toString());
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

	public void sendSubscribersList(Session session, int id) {
		Device device = findDeviceById(id);
		
		if(device != null) {
			//grab data from all other devices
			JsonProvider provider = JsonProvider.provider();
			JsonObject otherDevicesList = assembleOtherDevicesList(provider, device);
			//send json list of other devices to session which requested them
			sendToSession(session, otherDevicesList);
		}
		
	}

	private JsonObject assembleOtherDevicesList(JsonProvider provider, Device requestingDevice) {
		JsonArrayBuilder deviceArray = provider.createArrayBuilder();
		JsonObjectBuilder uberObjectBuilder = provider.createObjectBuilder();
		
		for(Device device : devices) {
			if(!(device.getId() == requestingDevice.getId())) {
				//other device than asking device so add to list
				deviceArray.add(createDeviceJsonObjectWithoutAction(device));
			}
		}
		
		JsonObject websocketMessage = uberObjectBuilder
				.add("action", Action.RETRIEVE_SUBSCRIBERS.toString().toLowerCase())
				.add("subscribers", deviceArray.build())
				.build();
		
		return websocketMessage;
	}
	
	private JsonObject createDeviceJsonObjectWithoutAction(Device device) {
		JsonObject deviceMessage = createDeviceJsonObjectBuilderWithoutAction(device).build();
		return deviceMessage;
	}
	
	private JsonObject createDeviceJsonObjectWithStatus(Device device, Action action) {
		JsonObjectBuilder deviceMessageBuilder = createDeviceJsonObjectBuilderWithoutAction(device);
        JsonObject deviceMessage = deviceMessageBuilder
        	   .add("action", action.toString().toLowerCase())
               .build();
		return deviceMessage;
	}
	
	private JsonObjectBuilder createDeviceJsonObjectBuilderWithoutAction(Device device) {
		 JsonProvider provider = JsonProvider.provider();
         JsonObjectBuilder deviceMessageBuilder = provider.createObjectBuilder()
                .add("id", device.getId())
                .add("name", device.getName())
                .add("type", device.getType())
                .add("status", device.getStatus())
                .add("modelDescription", device.getModelDescription())
                .add("osType", device.getOsType())
                .add("osVersion", device.getOsVersion());
		return deviceMessageBuilder;
	}
	
}
