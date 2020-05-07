package com.dar.nclientv2.api.components;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.JsonReader;
import android.util.JsonToken;

import com.dar.nclientv2.utility.Utility;

import java.io.IOException;
import java.io.StringReader;
import java.util.Date;

public class Comment implements Parcelable {
    private String userImageURL,username,body;
    private int id,posterId;
    private Date date;
    //private boolean specialUser;
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
        if(userImageURL.equals("https://i."+ Utility.getHost()+"/avatars/blank.png"))userImageURL=null;
        jr.endObject();
        if(close)jr.close();
    }
    public Comment(String tag) throws IOException {
        this(new JsonReader(new StringReader(removeCode(tag))), true);
    }
    private static String removeCode(String x){
        StringBuilder builder=new StringBuilder();
        char[]y=x.toCharArray();
        for(int i=0;i<y.length;i++){
            if(y[i]=='&'&&y[i+1]=='#'&&y[i+2]=='3'&&y[i+3]=='4'&&y[i+4]==';'){
                builder.append('"');
                i+=4;
            } else if(y[i]==3&&y[i+1]=='4'&&y[i+2]==';'){
                builder.append('"');
                i+=2;
            } else builder.append(y[i]);
        }
        return builder.toString();
    }
    protected Comment(Parcel in) {
        userImageURL = in.readString();
        username = in.readString();
        body = in.readString();
        id = in.readInt();
        posterId = in.readInt();
        date =new Date(in.readLong());
    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(userImageURL);
        dest.writeString(username);
        dest.writeString(body);
        dest.writeInt(id);
        dest.writeInt(posterId);
        dest.writeLong(date.getTime());
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
                case "avatar_url":userImageURL=jr.nextString().substring(30);break;
                default:jr.skipValue();break;
            }
        }
        jr.endObject();
    }

    public String getUserImageURL() {
        return "https://i."+Utility.getHost()+"/avatars/"+userImageURL;
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
        return date;
    }

    @Override
    public int describeContents() {
        return 0;
    }


}
