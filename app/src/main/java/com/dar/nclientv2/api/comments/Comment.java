package com.dar.nclientv2.api.comments;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.JsonReader;
import android.util.JsonToken;

import java.io.IOException;
import java.util.Date;

public class Comment implements Parcelable {
    public static final Creator<Comment> CREATOR = new Creator<Comment>() {
        @Override
        public Comment createFromParcel(Parcel in) {
            return new Comment(in);
        }

        @Override
        public Comment[] newArray(int size) {
            return new Comment[size];
        }
    };
    private int id;
    private User poster;
    private Date postDate;
    private String body;

    public Comment(JsonReader reader) throws IOException {
        reader.beginObject();
        while (reader.peek() != JsonToken.END_OBJECT) {
            switch (reader.nextName()) {
                case "id":
                    id = reader.nextInt();
                    break;
                case "post_date":
                    postDate = new Date(reader.nextLong() * 1000);
                    break;
                case "body":
                    body = reader.nextString();
                    break;
                case "poster":
                    poster = new User(reader);
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
    }

    protected Comment(Parcel in) {
        id = in.readInt();
        poster = in.readParcelable(User.class.getClassLoader());
        body = in.readString();
        postDate = new Date(in.readLong());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeParcelable(poster, flags);
        dest.writeString(body);
        dest.writeLong(postDate.getTime());
    }

    public int getId() {
        return id;
    }

    public User getPoster() {
        return poster;
    }

    public Date getPostDate() {
        return postDate;
    }

    public String getComment() {
        return body;
    }

    public int getPosterId() {
        return poster.getId();
    }

    public String getUsername() {
        return poster.getUsername();
    }
  
    public Uri getAvatarUrl(){
        return poster.getAvatarUrl();
    }
}
