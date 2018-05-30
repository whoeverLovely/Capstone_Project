package com.louise.udacity.mydict.data;

import android.os.Parcel;
import android.os.Parcelable;

public class ClientVocabulary implements Parcelable {
    private String word;
    private String phonetic;
    private String definition;
    private String translation;
    private String tag;
    private int status;
    private int groupId;

    public void setWord(String word) {
        this.word = word;
    }

    public void setPhonetic(String phonetic) {
        this.phonetic = phonetic;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public void setTranslation(String translation) {
        this.translation = translation;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public String getWord() {
        return word;
    }

    public String getPhonetic() {
        return phonetic;
    }

    public String getDefinition() {
        return definition;
    }

    public String getTranslation() {
        return translation;
    }

    public String getTag() {
        return tag;
    }

    public int getStatus() {
        return status;
    }

    public int getGroupId() {
        return groupId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.word);
        dest.writeString(this.phonetic);
        dest.writeString(this.definition);
        dest.writeString(this.translation);
        dest.writeString(this.tag);
        dest.writeInt(this.status);
        dest.writeInt(this.groupId);
    }

    public ClientVocabulary() {
    }

    protected ClientVocabulary(Parcel in) {
        this.word = in.readString();
        this.phonetic = in.readString();
        this.definition = in.readString();
        this.translation = in.readString();
        this.tag = in.readString();
        this.status = in.readInt();
        this.groupId = in.readInt();
    }

    public static final Parcelable.Creator<ClientVocabulary> CREATOR = new Parcelable.Creator<ClientVocabulary>() {
        @Override
        public ClientVocabulary createFromParcel(Parcel source) {
            return new ClientVocabulary(source);
        }

        @Override
        public ClientVocabulary[] newArray(int size) {
            return new ClientVocabulary[size];
        }
    };
}
