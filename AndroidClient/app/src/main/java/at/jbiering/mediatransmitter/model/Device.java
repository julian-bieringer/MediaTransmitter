package at.jbiering.mediatransmitter.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Device implements Parcelable{

    private long id;
    private String ip;
    private String name;
    private String status;
    private String type;
    private String modelDescription;
    private String osType;
    private String osVersion;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(id);
        parcel.writeString(ip);
        parcel.writeString(name);
        parcel.writeString(status);
        parcel.writeString(type);
        parcel.writeString(modelDescription);
        parcel.writeString(osType);
        parcel.writeString(osVersion);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Device createFromParcel(Parcel in) {
            long id = in.readLong();
            String ip = in.readString();
            String name = in.readString();
            String status = in.readString();
            String type = in.readString();
            String modelDescription = in.readString();
            String osType = in.readString();
            String osVersion = in.readString();

            return new Device(id, ip, name, status, type, modelDescription, osType, osVersion);
        }

        public Device[] newArray(int size) {
            return new Device[size];
        }
    };

    public Device(long id, String ip, String name, String status, String type, String modelDescription, String osType, String osVersion) {
        this.id = id;
        this.ip = ip;
        this.name = name;
        this.status = status;
        this.type = type;
        this.modelDescription = modelDescription;
        this.osType = osType;
        this.osVersion = osVersion;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
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
