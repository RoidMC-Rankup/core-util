package com.roidmc.coreutil.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.roidmc.coreutil.exceptions.WebSocketReadException;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

public class WebSocket {

    private final String ip;
    private final int port;
    private Gson gson = new Gson();

    private final BlockingQueue<WSMessage> responsesQueue = new LinkedBlockingDeque<>();
    private final Map<String, WSMessage> waitResponse = new ConcurrentHashMap<>();
    private final BlockingQueue<JsonObject> requestsQueue = new LinkedBlockingDeque<>();
    private final Map<String, WSChannel> channels = new ConcurrentHashMap<>();

    public WebSocket(String ip, int port) {
        this.ip = ip;
        this.port = port;

        new Thread(()->{
            while(true){
                JsonObject object= null;
                try {
                    object = requestsQueue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
                String replyTo = object.has("reply_to")?object.get("reply_to").getAsString():null;
                WSChannel channel = findChannel(object.has("channel")?object.get("channel").getAsString():"default");
                String sender = object.has("sender")?object.get("sender").getAsString():"Not defined";
                WSMessage message = new WSMessage(channel,sender) {
                    WSMessage messageReply = null;
                    @Override
                    public WSMessage reply(String sender) {
                        if(messageReply==null){
                            messageReply = channel.createMessage(sender);
                            reply(messageReply);
                        }
                        return messageReply;
                    }

                    @Override
                    public void send() {}

                    @Override
                    public void send(Runnable cb) {
                        cb.run();
                    }
                };
                message.load(object);
                WSMessage replyMessage = replyTo!=null&&!replyTo.isEmpty()?waitResponse.get(replyTo):null;
                if(replyMessage!=null){
                    replyMessage.reply(message);
                    waitResponse.remove(replyTo);
                }
                channel.call(message);

            }
        }).start();
        connect();
    }

    public WSChannel findChannel(String name){
        assert name!=null && !name.isEmpty();
        name = name.toLowerCase();
        return channels.computeIfAbsent(name,(n)->new WSChannel(n){
            @Override
            protected void send(WSMessage request) {
                responsesQueue.add(request);
            }
        });
    }

    public void sendError(Throwable e){
        e.printStackTrace();
    }

    public void sendError(String msg){
        System.out.println(msg);
    }

    private void connect(){
        new Thread(()->{
            try (Socket socket = new Socket(this.ip,this.port)){
                Thread read = new Thread(()->{
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        String line;
                        while((line = reader.readLine())!=null){
                            try {
                                JsonObject object = gson.fromJson(line, JsonObject.class);
                                requestsQueue.add(object);
                            } catch (RuntimeException ignored){} catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                        System.out.println("Read stoped");
                    }catch (Exception ignored){}
                });
                Thread write = new Thread(()->{
                    try {
                        PrintWriter writer = new PrintWriter(socket.getOutputStream(),true);
                        while(!Thread.currentThread().isInterrupted()){
                            WSMessage response = responsesQueue.take();
                            if(response.replyTo!=null){
                                response.request.addProperty("reply_to",response.replyTo.ref);
                            }
                            waitResponse.put(response.ref,response);
                            writer.println(gson.toJson(response.request));
                            response.sent=true;
                        }
                    }catch (Exception ignored){}
                });
                read.start();
                write.start();
                while(socket.isConnected()){
                    Thread.sleep(1000);
                }
                read.interrupt();
                write.interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("[WebSocket] "+this.getClass().getCanonicalName()+" disconnected, reconnecting...");
            connect();
        }).start();
    }



}
