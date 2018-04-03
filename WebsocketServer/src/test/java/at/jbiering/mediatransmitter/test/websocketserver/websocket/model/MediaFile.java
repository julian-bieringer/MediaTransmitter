package at.jbiering.mediatransmitter.test.websocketserver.websocket.model;

public class MediaFile {
	
	private String bytesBase64;
	private String fileExtension;
	private String fileName;
	
	public MediaFile(String bytesBase64, String fileExtension, String fileName) {
		this.bytesBase64 = bytesBase64;
		this.fileExtension = fileExtension;
		this.fileName = fileName;
	}

	public String getBytesBase64() {
		return bytesBase64;
	}

	public void setBytesBase64(String bytesBase64) {
		this.bytesBase64 = bytesBase64;
	}

	public String getFileExtension() {
		return fileExtension;
	}

	public void setFileExtension(String fileExtension) {
		this.fileExtension = fileExtension;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
}
