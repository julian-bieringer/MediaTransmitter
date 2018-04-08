package at.jbiering.mediatransmitter.model;

public class MediaFile {

    byte[] bytes;
    String fileName;
    String fileExtension;

    public MediaFile() {
    }

    public MediaFile(byte[] bytes, String fileName, String fileExtension) {
        this.bytes = bytes;
        this.fileName = fileName;
        this.fileExtension = fileExtension;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }
}
