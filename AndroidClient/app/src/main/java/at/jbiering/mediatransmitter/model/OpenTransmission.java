package at.jbiering.mediatransmitter.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class OpenTransmission implements Parcelable {

    private Uri uri;
    private long recipientId;
    private String md5Checksum;

    public OpenTransmission() {
    }

    public OpenTransmission(Uri uri, long recipientId) {
        this.uri = uri;
        this.recipientId = recipientId;
    }

    public OpenTransmission(Uri uri, long recipientId, String md5Checksum) {
        this.uri = uri;
        this.recipientId = recipientId;
        this.md5Checksum = md5Checksum;
    }

    protected OpenTransmission(Parcel in) {
        uri = in.readParcelable(Uri.class.getClassLoader());
        recipientId = in.readLong();
        md5Checksum = in.readString();
    }

    public static final Creator<OpenTransmission> CREATOR = new Creator<OpenTransmission>() {
        @Override
        public OpenTransmission createFromParcel(Parcel in) {
            Uri uri = Uri.parse(in.readString());
            String md5Checksum = in.readString();
            long recipientId = in.readLong();
            OpenTransmission openTransmission = new OpenTransmission(uri, recipientId, md5Checksum);
            return openTransmission;
        }

        @Override
        public OpenTransmission[] newArray(int size) {
            return new OpenTransmission[size];
        }
    };

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(long recipientId) {
        this.recipientId = recipientId;
    }

    public String getMd5Checksum() {
        return md5Checksum;
    }

    public void setMd5Checksum(String md5Checksum) {
        this.md5Checksum = md5Checksum;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(uri.toString());
        parcel.writeString(md5Checksum);
        parcel.writeLong(recipientId);
    }
}
