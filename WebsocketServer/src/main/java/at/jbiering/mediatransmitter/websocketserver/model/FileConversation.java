package at.jbiering.mediatransmitter.websocketserver.model;

public class FileConversation {

	private long senderDeviceId;
	private long recipientDeviceId;
	
	public FileConversation(long senderDeviceId, long recipientDeviceId) {
		super();
		this.senderDeviceId = senderDeviceId;
		this.recipientDeviceId = recipientDeviceId;
	}

	public long getSenderDeviceId() {
		return senderDeviceId;
	}

	public void setSenderDeviceId(long senderDeviceId) {
		this.senderDeviceId = senderDeviceId;
	}

	public long getRecipientDeviceId() {
		return recipientDeviceId;
	}

	public void setRecipientDeviceId(long recipientDeviceId) {
		this.recipientDeviceId = recipientDeviceId;
	}
	
}
