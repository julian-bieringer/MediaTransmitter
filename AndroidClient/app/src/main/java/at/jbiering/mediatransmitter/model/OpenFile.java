package at.jbiering.mediatransmitter.model;

public class OpenFile {

    private String filePath;
    private int fileParts;
    private boolean[] filePartsReceived;
    //remember which indices where written to the tmp file with order
    private int[] filePartIndices;

    public OpenFile() {
    }

    public OpenFile(String filePath, int fileParts, boolean[] filePartsReceived, int[] filePartIndices) {
        this.filePath = filePath;
        this.fileParts = fileParts;
        this.filePartsReceived = filePartsReceived;
        this.filePartIndices = filePartIndices;
    }

    public boolean[] getFilePartsReceived() {
        return filePartsReceived;
    }

    public void setFilePartsReceived(boolean[] filePartsReceived) {
        this.filePartsReceived = filePartsReceived;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getFileParts() {
        return fileParts;
    }

    public void setFileParts(int fileParts) {
        this.fileParts = fileParts;
    }

    public int[] getFilePartIndices() {
        return filePartIndices;
    }

    public void setFilePartIndices(int[] filePartIndices) {
        this.filePartIndices = filePartIndices;
    }
}
