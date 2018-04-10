package at.jbiering.mediatransmitter.websocketserver.websocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import at.jbiering.mediatransmitter.websocketserver.model.FileConversation;
import at.jbiering.mediatransmitter.websocketserver.model.enums.Action;

@ApplicationScoped
public class SessionHandler {

	@Inject
	private Logger logger;
	
	private final Set<Session> sessions = new HashSet<>();
	private final Set<Device> devices = new HashSet<>();
	private Map<String, FileConversation> openFileConversations;
	private long deviceId = 1;
	
	public void addSession(Session session) {
		sessions.add(session);
	}
	
	public void removeSession(Session session) {
		logger.info("*** removing session now ***");
		Session setSession = findSessionBySessionId(session.getId());
		
		Device device = findDeviceBySessionId(session.getId());
		if(device != null) {
			//something went wrong as device is still there -> remove message has been omitted
			//-> probably client crashed or closed connection abruptly -> remove device
			removeDevice(session);
		}
		
		//device removed -> can now safely remove session
		if(setSession != null)
			sessions.remove(setSession);
	}
	
	public List<Device> getDevices(){
		return new ArrayList<>(devices);
	}
	
	public List<Session> getSessions() {
		return new ArrayList<>(sessions);
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

		sendToEverySessionButCurrent(updateRequiredMessage, session.getId());

		this.devices.add(device);
	}
	
	private JsonObject createSubscriberUpdateRequestedMessage() {
		JsonProvider provider = JsonProvider.provider();
		JsonObject object = provider.createObjectBuilder()
				.add("action", "subscriber_list_update_required")
				.build();
		return object;
	}

	public void removeDevice(Session session) {
		if(session != null) {
			Device device = findDeviceBySessionId(session.getId());
			if(device != null) {
				devices.remove(device);
				sendToEverySessionButCurrent(createSubscriberUpdateRequestedMessage(), session.getId());
			}
		}
	}

	private void sendToEverySessionButCurrent(JsonObject message, String currentSessionId) {
		for(Session curr : sessions) {
			if(!(curr.getId().equals(currentSessionId))) {
				sendToSession(curr, message);
			}
		}
	}
	
	private Session findSessionBySessionId(String sessionId) {
		for(Session session : sessions) {
			if(session.getId().equals(sessionId))
				return session;
		}
		return null;
	}

	public void toggleDevice(Session session) {
		JsonProvider provider = JsonProvider.provider();
		Device device = findDeviceBySessionId(session.getId());
		
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
    		logger.info("*** Sending message with action [" + message.getString("action") + "] to client with session id [" + session.getId() + "] ***");
    		logger.info("*** sending " + message.toString().length() + " bytes with max buffer size of " + session.getMaxTextMessageBufferSize() + " bytes ***");
			session.getBasicRemote().sendText(message.toString());
			logger.info("*** Sending finished ***");
		} catch (IOException e) {
			//session is no longer active so remove session and according device
			Session setSession = findSessionBySessionId(session.getId());
			sessions.remove(setSession);
			
			Device device = findDeviceBySessionId(session.getId());
			if(device != null)
				this.devices.remove(device);
			
			logger.error("*** Sending message with action [" + message.getString("action") + "] to client with session id [" + session.getId() + "] failed ***");
		}
    }
	
	
	private Device findDeviceBySessionId(String sessionId) {
		for(Device device : devices) {
			if(device.getSessionId().equals(sessionId))
				return device;
		}
		return null;
	}

	public void sendSubscribersList(Session session) {
		Device device = findDeviceBySessionId(session.getId());
		
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
                .add("ip", device.getIp())
                .add("name", device.getName())
                .add("type", device.getType())
                .add("status", device.getStatus())
                .add("modelDescription", device.getModelDescription())
                .add("osType", device.getOsType())
                .add("osVersion", device.getOsVersion());
		return deviceMessageBuilder;
	}
	
	private Session findSessionByDeviceId(long deviceId) {
		for(Device device : devices) {
			if(device.getId() == deviceId) {
				return findSessionBySessionId(device.getSessionId());
			}
		}
		return null;
	}

	public void sendCreateFileMessage(JsonObject jsonMessage, Session session) {
		long recipientId = readRecipientIdFromJsonObject(jsonMessage);
		long senderId = findDeviceBySessionId(session.getId()).getId();
		String transferUuid = jsonMessage.getString("transfer_uuid");
		
		if(openFileConversations == null)
			openFileConversations = new HashMap<>();
		
		openFileConversations.put(transferUuid, new FileConversation(senderId, recipientId));
		
		 JsonProvider provider = JsonProvider.provider();
         JsonObject createFileMessage = provider.createObjectBuilder()
                .add("action", Action.CREATE_FILE.toString().toLowerCase())
                .add("file_extension", jsonMessage.getString("file_extension"))
                .add("file_parts", jsonMessage.getInt("file_parts"))
                .add("transfer_uuid", jsonMessage.getString("transfer_uuid"))
                .build();
  
		
		Session recipientSession = findSessionByDeviceId(recipientId);
		if(recipientSession != null)
			sendToSession(recipientSession, createFileMessage);
	}

	public void sendFilePartMessage(JsonObject jsonMessage) {
		String fileTransferUuid = jsonMessage.getString("transfer_uuid");
		FileConversation fileConversation = openFileConversations.get(fileTransferUuid);
		long recipientId = fileConversation.getRecipientDeviceId();
		
		JsonProvider provider = JsonProvider.provider();
		JsonObject retrieveFileMessage = provider
				.createObjectBuilder()
				.add("action", Action.RETRIEVE_FILE_PART.toString().toLowerCase())
				.add("index", jsonMessage.getInt("index"))
				.add("transfer_uuid", fileTransferUuid)
				.add("bytes_base64", jsonMessage.getString("bytes_base64"))
				.build();
		
		Session recipientSession = findSessionByDeviceId(recipientId);
		if(recipientSession != null)
			sendToSession(recipientSession, retrieveFileMessage);
	}
	
	private long readRecipientIdFromJsonObject(JsonObject jsonObject) {
		long recipientId = jsonObject.getInt("recipient_id");
		return recipientId;
	}

	public void sendReceivedFileMessage(JsonObject jsonMessage) {
		FileConversation fileConversation = findFileConversationByTransferUuidInJsonObject(jsonMessage);
		
		Session recipientSession = findSessionByDeviceId(fileConversation.getSenderDeviceId());
		if(recipientSession != null)
			sendToSession(recipientSession, jsonMessage);
	}

	public void sendEndFileRequestMessage(JsonObject jsonMessage) {
		FileConversation fileConversation = findFileConversationByTransferUuidInJsonObject(jsonMessage);
		
		Session recipientSession = findSessionByDeviceId(fileConversation.getRecipientDeviceId());
		if(recipientSession != null)
			sendToSession(recipientSession, jsonMessage);
	}

	public void sendEndFileErrorOrAcknowledgedMessage(JsonObject jsonMessage) {
		String fileTransferUuid = jsonMessage.getString("transfer_uuid");
		FileConversation fileConversation = openFileConversations.get(fileTransferUuid);
		
		Session recipientSession = findSessionByDeviceId(fileConversation.getSenderDeviceId());
		if(recipientSession != null)
			sendToSession(recipientSession, jsonMessage);
		
		openFileConversations.remove(fileTransferUuid);
	}
	
	private FileConversation findFileConversationByTransferUuidInJsonObject(JsonObject jsonMessage) {
		String fileTransferUuid = jsonMessage.getString("transfer_uuid");
		FileConversation fileConversation = openFileConversations.get(fileTransferUuid);
		return fileConversation;
	}

	public void sendCreateFileAcknowledgedMessage(JsonObject jsonMessage) {
		FileConversation fileConversation = findFileConversationByTransferUuidInJsonObject(jsonMessage);
		
		Session recipientSession = findSessionByDeviceId(fileConversation.getSenderDeviceId());
		if(recipientSession != null)
			sendToSession(recipientSession, jsonMessage);
	}
	
}
