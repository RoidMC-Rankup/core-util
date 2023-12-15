package com.roidmc.coreutil.websocket;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.roidmc.coreutil.ArrayUtil;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public abstract class WSChannel {


    private final String name;
    private final CopyOnWriteArrayList<Consumer<WSMessage>> listeners = new CopyOnWriteArrayList<>();

    protected WSChannel(String name) {
        this.name = name;
    }

    protected boolean isChannel(JsonObject object){
        return object.get("event").getAsString().equals(name);
    }

    protected void call(WSMessage message){
        for(Consumer<WSMessage> consumer : listeners){
            try {
                consumer.accept(message);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void subscribe(Consumer<WSMessage> consumer){
        this.listeners.add(consumer);
    }
    protected abstract void send(WSMessage request);

    public WSMessage createMessage(String sender){
        return new WSMessage(this,sender){
            Runnable runnable = ()->{};

            @Override
            public void send() {
                WSChannel.this.send(this);
            }

            @Override
            public void send(Runnable cb) {
                runnable = cb;
                send();
            }

            @Override
            protected void reply(WSMessage wsMessage) {
                super.reply(wsMessage);
                runnable.run();
            }
        };
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "WSChannel{" +
                "name='" + name + '\'' +
                ", listeners=" + listeners.size() +
                '}';
    }
}
