package com.dar.nclientv2.api.components;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import com.dar.nclientv2.settings.Global;

import java.io.IOException;
import java.io.StringReader;
import java.util.Date;

public class Comment implements Parcelable {
    private String userImageURL,username,body;
    private int id,posterId;
    private Date date;
    private boolean specialUser;
    public Comment(JsonReader jr,boolean close) throws IOException{
        jr.beginObject();
        while(jr.peek()!= JsonToken.END_OBJECT){
            switch (jr.nextName()){
                case "id":id=jr.nextInt();break;
                case "post_date":date=new Date(jr.nextLong()*1000L);break;
                case "body":body=jr.nextString();break;
                case "poster":parseUser(jr);break;
                default:jr.skipValue();break;
            }
        }
        if(userImageURL.equals("https://i.nhentai.net/avatars/blank.png"))userImageURL=null;
        jr.endObject();
        if(close)jr.close();
    }
    public Comment(String tag) throws IOException {
        this(new JsonReader(new StringReader(tag.replace("&#34;","\""))),true);

    }

    protected Comment(Parcel in) {
        userImageURL = in.readString();
        username = in.readString();
        body = in.readString();
        id = in.readInt();
        posterId = in.readInt();
        date =new Date(in.readLong());
        specialUser = in.readByte() != 0;
    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(userImageURL);
        dest.writeString(username);
        dest.writeString(body);
        dest.writeInt(id);
        dest.writeInt(posterId);
        dest.writeLong(date.getTime());
        dest.writeByte((byte)(specialUser?1:0));
    }
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

    private void parseUser(JsonReader jr)throws IOException {
        jr.beginObject();
        while(jr.peek()!= JsonToken.END_OBJECT){
            switch (jr.nextName()){
                case "id":posterId=jr.nextInt();break;
                case "username":username=jr.nextString();break;
                case "avatar_url":userImageURL=jr.nextString();break;
                case "is_superuser": case "is_staff":boolean x=jr.nextBoolean();if(!specialUser)specialUser=x;break;
                default:jr.skipValue();break;
            }
        }
        jr.endObject();
    }

    public String getUserImageURL() {
        return userImageURL;
    }

    public String getUsername() {
        return username;
    }

    public String getBody() {
        return body;
    }

    public int getId() {
        return id;
    }

    public int getPosterId() {
        return posterId;
    }

    public Date getDate() {
        Log.d(Global.LOGTAG,id+","+date.getTime());
        return date;
    }

    public boolean isSpecialUser() {
        return specialUser;
    }

    @Override
    public int describeContents() {
        return 0;
    }


}
