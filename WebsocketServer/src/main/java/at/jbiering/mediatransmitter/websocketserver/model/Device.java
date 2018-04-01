package at.jbiering.mediatransmitter.websocketserver.model;

public class Device {
	
	private long id;
    private String name;
    private String status;
    private String type;
    private String description;

    public Device() {
    }
    
    public Device(String name, String status, String type, String description) {
		this.name = name;
		this.status = status;
		this.type = type;
		this.description = description;
	}

	public long getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public String getType() {
        return type;
    }
    
    public String getDescription() {
        return description;
    }

    public void setId(long id) {
        this.id = id;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setType(String type) {
        this.type = type;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
}
