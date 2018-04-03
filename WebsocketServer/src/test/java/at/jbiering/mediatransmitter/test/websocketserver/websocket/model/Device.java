package at.jbiering.mediatransmitter.test.websocketserver.websocket.model;

public class Device {
	
	private long id;
    private String name;
    private String status;
    private String type;
    private String modelDescription;
    private String osType;
    private String osVersion;
    
    public Device() {
    }

	public Device(long id, String name, String status, String type, String modelDescription, String osType,
			String osVersion) {
		this.id = id;
		this.name = name;
		this.status = status;
		this.type = type;
		this.modelDescription = modelDescription;
		this.osType = osType;
		this.osVersion = osVersion;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getModelDescription() {
		return modelDescription;
	}

	public void setModelDescription(String modelDescription) {
		this.modelDescription = modelDescription;
	}

	public String getOsType() {
		return osType;
	}

	public void setOsType(String osType) {
		this.osType = osType;
	}

	public String getOsVersion() {
		return osVersion;
	}

	public void setOsVersion(String osVersion) {
		this.osVersion = osVersion;
	}
}