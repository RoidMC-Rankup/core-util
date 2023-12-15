package com.roidmc.coreutil.websocket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.roidmc.coreutil.CollectorsUtil;
import com.roidmc.coreutil.EntryImpl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public abstract class WSMessage {

    protected String ref;
    protected boolean sent;
    protected JsonObject request;
    private JsonObject header;
    private JsonElement body;
    protected WSMessage reply;
    protected WSMessage replyTo;
    public final WSChannel channel;
    private String sender;
    public final long created_at;



    protected WSMessage(WSChannel channel, String sender) {
        this.ref = UUID.randomUUID().toString();
        this.channel = channel;
        this.request = new JsonObject();
        this.header = new JsonObject();
        this.body = new JsonObject();
        this.sender = sender;
        this.created_at = System.currentTimeMillis();
        this.request.addProperty("channel",channel!=null?channel.getName():"default");
        this.request.addProperty("sender", sender);
        this.request.addProperty("ref",ref);
        this.request.add("header",this.header);
        this.request.add("body",this.body);
    }

    protected void load(JsonObject request){
        this.ref = request.has("ref")?request.get("ref").getAsString():this.ref;
        this.sender = request.has("sender")?request.get("sender").getAsString():this.ref;
        this.request = request;
        this.header = request.has("header")?request.get("header").getAsJsonObject():new JsonObject();
        this.body = request.has("body")?request.get("body"):new JsonObject();
        this.request.add("header",this.header);
        this.request.add("body",this.body);
    }

    public WSMessage reply(String sender){
        return null;
    }

    protected void reply(WSMessage wsMessage){
        this.reply = wsMessage;
        wsMessage.replyTo = this;
    }

    public abstract void send();
    public abstract void send(Runnable cb);

    public boolean isSent() {
        return sent;
    }

    public WSMessage getReplyTo() {
        return replyTo;
    }

    public WSMessage getReply() {
        return reply;
    }

    public JsonElement getBody(){
        return body.deepCopy();
    }

    public JsonElement getHeader(String key){
        JsonElement element = this.header.get(key);
        return element!=null?element.deepCopy():null;
    }

    public JsonObject getHeader(){
        return this.header.deepCopy();
    }
    public WSMessage setHeader(String key, JsonElement value){
        this.header.add(key,value);
        return this;
    }
    public WSMessage setHeader(String key, String value){
        return this.setHeader(key,new JsonPrimitive(value));
    }
    public WSMessage setHeader(String key, Number value){
        return this.setHeader(key,new JsonPrimitive(value));
    }
    public WSMessage setHeader(String key, Boolean value){
        return this.setHeader(key,new JsonPrimitive(value));
    }
    public WSMessage addHeader(String key, JsonElement value){
        if(!this.header.has(key)){
            this.header.add(key,new JsonArray());
        }else if(!this.header.get(key).isJsonArray()){
            JsonArray array = new JsonArray();
            array.add(this.header.get(key));
            this.header.add(key,array);
        }
        this.header.get(key).getAsJsonArray().add(value);
        return this;
    }
    public WSMessage addHeader(String key, JsonArray value){
        JsonElement element = this.header.get(key);
        if(element==null){
            this.header.add(key,element = new JsonArray());
        }else if(!element.isJsonArray()){
            JsonArray array = new JsonArray();
            array.add(element);
            this.header.add(key,element = array);
        }
        element.getAsJsonArray().addAll(value);
        return this;
    }
    public WSMessage addHeader(String key, String... values){
        return this.addHeader(key, Arrays.stream(values).map(JsonPrimitive::new).collect(CollectorsUtil.toJsonArray()));
    }
    public WSMessage addHeader(String key, Number... values){
        return this.addHeader(key, Arrays.stream(values).map(JsonPrimitive::new).collect(CollectorsUtil.toJsonArray()));
    }
    public WSMessage addHeader(String key, Boolean... values){
        return this.addHeader(key, Arrays.stream(values).map(JsonPrimitive::new).collect(CollectorsUtil.toJsonArray()));
    }

    public WSMessage setBody(JsonElement element){
        this.body = element;
        request.add("body",element);
        return this;
    }
    public WSMessage setBody(JsonElement... elements){
        return setBody(Arrays.stream(elements).collect(CollectorsUtil.toJsonArray()));
    }

    public WSMessage setBody(String... texts){
        return setBody(Arrays.stream(texts).map(JsonPrimitive::new).collect(CollectorsUtil.toJsonArray()));

    }
    public WSMessage setBody(Number... numbers){
        return setBody(Arrays.stream(numbers).map(JsonPrimitive::new).collect(CollectorsUtil.toJsonArray()));

    }
    public WSMessage setBody(Boolean... bools){
        return setBody(Arrays.stream(bools).map(JsonPrimitive::new).collect(CollectorsUtil.toJsonArray()));

    }
    public WSMessage setBodyStrings(Collection<String> collection){
        return setBody(collection.stream().map(JsonPrimitive::new).collect(CollectorsUtil.toJsonArray()));

    }
    public WSMessage setBodyNumbers(Collection<? extends Number> collection){
        return setBody(collection.stream().map(JsonPrimitive::new).collect(CollectorsUtil.toJsonArray()));

    }
    public WSMessage setBodyBooleans(Collection<Boolean> collection){
        return setBody(collection.stream().map(JsonPrimitive::new).collect(CollectorsUtil.toJsonArray()));

    }
    public WSMessage setBody(Map<String,String> map){
        return setBody(map.entrySet().stream().map((entry)->new EntryImpl<String, JsonElement>(entry.getKey(),new JsonPrimitive(entry.getValue()))).collect(CollectorsUtil.toJsonObject()));

    }
    public WSMessage setBodyNumbers(Map<String,Number> map){
        return setBody(map.entrySet().stream().map((entry)->new EntryImpl<String,JsonElement>(entry.getKey(),new JsonPrimitive(entry.getValue()))).collect(CollectorsUtil.toJsonObject()));

    }
    public WSMessage setBodyBooleans(Map<String,Boolean> map){
        return setBody(map.entrySet().stream().map((entry)->new EntryImpl<String,JsonElement>(entry.getKey(),new JsonPrimitive(entry.getValue()))).collect(CollectorsUtil.toJsonObject()));

    }

    @Override
    public String toString() {
        return "WSMessage2{" +
                "ref='" + ref + '\'' +
                ", request=" + request +
                ", header=" + header +
                ", body=" + body +
                ", reply=" + reply +
                ", channel=" + channel +
                ", sender='" + sender + '\'' +
                ", created_at=" + created_at +
                '}';
    }
}
