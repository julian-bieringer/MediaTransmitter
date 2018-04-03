package at.jbiering.mediatransmitter.test.websocketserver.websocket;

import java.io.IOException;
import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.spi.JsonProvider;
import javax.websocket.ClientEndpoint;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import at.jbiering.mediatransmitter.test.websocketserver.websocket.model.Device;
import at.jbiering.mediatransmitter.test.websocketserver.websocket.model.MediaFile;
import at.jbiering.mediatransmitter.websocketserver.model.enums.Action;

@ClientEndpoint
public class DeviceClientEndpoint {
		
	private Device currentDevice;
	private Session session;
	private Device[] devices;
	private String deviceName;
	private MediaFile mediaFile;
	
    public DeviceClientEndpoint(String deviceName) {
		this.deviceName = deviceName;
	}

	public Device[] getDevices() {
    	Device[] copyDevices = new Device[devices.length];
		System.arraycopy(devices, 0, copyDevices, 0, devices.length);
		return copyDevices;
	}

	public void setDevices(Device[] devices) {
		this.devices = devices;
	}

	@OnOpen
    public void onOpen(Session session) {
    	this.session = session;

        try {
        	String action = "add";
            String name = deviceName;
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
	
	@OnClose
	public void onClose() {
		this.session = null;
		System.out.println("connection closed");
	}
    
    public void sendMessage(JsonObject message) {
    	try {
			session.getBasicRemote().sendText(message.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    //set message limit to 100 megabyte
    @OnMessage(maxMessageSize = 100 * 1024 * 1024)
    public void processMessage(String message) {

        try (JsonReader reader = Json.createReader(new StringReader(message))){
			JsonObject jsonMessage = reader.readObject();
			String actionString = jsonMessage.getString("action");
			Action action = Enum.valueOf(Action.class, actionString.toUpperCase());
			
	        System.out.println("*** Received message in client with action [" + action.toString() + "] ***");
			
			if(action.equals(Action.ADD)) {
				this.currentDevice = extractDeviceInfoFromJsonObject(jsonMessage);
			} else if(action.equals(Action.RETRIEVE_SUBSCRIBERS)) {
				updateSubscriberArray(jsonMessage);
			} else if(action.equals(Action.RETRIEVE_FILE)) {
				this.mediaFile = retrieveByteArray(jsonMessage);
			}
        }
    	WebsocketServerTests.messageLatch.countDown();
    }

    private MediaFile retrieveByteArray(JsonObject jsonMessage) {
		String bytesBase64 = jsonMessage.getString("bytes_base64");
		String fileName = jsonMessage.getString("file_name");
		String fileExtension = jsonMessage.getString("file_extension");
		return new MediaFile(bytesBase64, fileExtension, fileName);
	}

	private void updateSubscriberArray(JsonObject jsonMessage) {
    	JsonArray subscribers = jsonMessage.getJsonArray("subscribers");
    	
    	this.devices = new Device[subscribers.size()];
    	int index = 0;
		
		for(JsonValue subscriber : subscribers) {
			JsonObject subscriberObject = (JsonObject) subscriber;
			this.devices[index] = extractDeviceInfoFromJsonObject(subscriberObject);
			index++;
		}
	}
    
    private Device extractDeviceInfoFromJsonObject(JsonObject jsonObject) {
    	long id = jsonObject.getInt("id");
    	String name = jsonObject.getString("name");
    	String status = jsonObject.getString("status");
		String modelDescription = jsonObject.getString("modelDescription");
		String type = jsonObject.getString("type");
		String osType = jsonObject.getString("osType");
		String osVersion = jsonObject.getString("osVersion");
		return new Device(id, name, status, type, modelDescription, osType, osVersion);
    }

	@OnError
    public void processError(Throwable t) {
        t.printStackTrace();
    }

	public Device getCurrentDevice() {
		return currentDevice;
	}

	public void setCurrentDevice(Device currentDevice) {
		this.currentDevice = currentDevice;
	}

	public MediaFile getMediaFile() {
		return mediaFile;
	}

	public void setMediaFile(MediaFile mediaFile) {
		this.mediaFile = mediaFile;
	}

	public void closeConnection() {
		try {
			this.session.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}