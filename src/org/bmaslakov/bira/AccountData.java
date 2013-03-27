package org.bmaslakov.bira;

import android.os.Parcel;
import android.os.Parcelable;

public class AccountData implements Parcelable {
    private String mConsumerKey;
    private String mConsumerSecret;

    private final String mTargetUrl;

    public AccountData(String accountName, String repoSlug, String consumerKey, String consumerSecret) {
        this.mConsumerKey = consumerKey;
        this.mConsumerSecret = consumerSecret;
        this.mTargetUrl = "https://api.bitbucket.org/1.0/repositories/" + accountName + "/" + repoSlug + "/issues";
    }

    public AccountData(Parcel in) {
        super();
        this.mConsumerKey = in.readString();
        this.mConsumerSecret = in.readString();
        this.mTargetUrl = in.readString();
    }

    public String getConsumerKey() {
        return mConsumerKey;
    }

    public String getConsumerSecret() {
        return mConsumerSecret;
    }

    public String getTargetUrl() {
        return mTargetUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mConsumerKey);
        dest.writeString(mConsumerSecret);
        dest.writeString(mTargetUrl);
    }

    public static final Parcelable.Creator<AccountData> CREATOR = new Parcelable.Creator<AccountData>() {
        public AccountData createFromParcel(Parcel in) {
            return new AccountData(in);
        }

        public AccountData[] newArray(int size) {
            return new AccountData[size];
        }
    };
}
