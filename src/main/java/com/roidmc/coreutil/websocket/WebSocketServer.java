package com.roidmc.coreutil.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.roidmc.coreutil.exceptions.WebSocketReadException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;

public class WebSocketServer {

    private final int port;
    private Gson gson = new Gson();

    private final BlockingQueue<WSMessage> responsesQueue = new LinkedBlockingDeque<>();
    private final Map<String, WSMessage> waitResponse = new ConcurrentHashMap<>();
    private final BlockingQueue<JsonObject> requestsQueue = new LinkedBlockingDeque<>();
    private final Map<String, WSChannel> channels = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Client> clients = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<ConnectionEvent>> handleConnections = new CopyOnWriteArrayList<>();

    public WebSocketServer(int port) {
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
        new Thread(()->{
            while(true){
                WSMessage response = null;
                try {
                    response = responsesQueue.take();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if(clients.size()==0){
                    responsesQueue.add(response);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    continue;
                }
                if(response.replyTo!=null){
                    response.request.addProperty("reply_to",response.replyTo.ref);
                }
                waitResponse.put(response.ref,response);
                String responseData = gson.toJson(response.request);
                for(Client client : clients){
                    try {
                        client.writer.println(responseData);
                    }catch (Exception ignored){}
                }
                response.sent=true;
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

    public WebSocketServer handleConnection(Consumer<ConnectionEvent> event){
        this.handleConnections.add(event);
        return this;
    }

    public void sendError(Throwable e){
        e.printStackTrace();
    }

    public void sendError(String msg){
        System.out.println(msg);
    }

    private void connect(){
        new Thread(()->{
            try (ServerSocket server = new ServerSocket(this.port)) {
                Socket socket;
                while((socket = server.accept())!=null){
                    Client client = new Client(socket);
                    clients.add(client);
                    ConnectionEvent connectionEvent = new ConnectionEvent(client,true);
                    for(Consumer<ConnectionEvent> consumer : handleConnections){
                        try {
                            consumer.accept(connectionEvent);
                        }catch (Exception e){e.printStackTrace();}
                    }

                    new Thread(()->{
                        try {
                            String line;
                            while ((line = client.reader.readLine()) != null) {
                                try {
                                    JsonObject object = gson.fromJson(line, JsonObject.class);
                                    requestsQueue.add(object);
                                } catch (RuntimeException ignored) {
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }catch (Exception ignored){}
                        try {
                            client.close();
                        } catch (IOException ignored) {}
                    }).start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("[WebSocket Server] "+this.getClass().getCanonicalName()+" disconnected, reconnecting...");
            connect();
        }).start();
    }


    public class Client {

        public final String id;
        protected final Socket socket;
        public final PrintWriter writer;
        public final BufferedReader reader;
        public final Map<String, Object> metadata = new ConcurrentHashMap<>();

        protected Client(Socket socket) throws IOException {
            this.id = UUID.randomUUID().toString();
            this.socket = socket;
            this.writer = new PrintWriter(socket.getOutputStream(),true);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        public void close() throws IOException {
            ConnectionEvent event = new ConnectionEvent(this,false);
            handleConnections.forEach(consumer->{
                consumer.accept(event);
            });
            this.writer.close();
            this.reader.close();
            if(!this.socket.isClosed())this.socket.close();
            clients.remove(this);
        }
    }


    public static class ConnectionEvent{

        public final Client client;
        public final boolean connect;

        public ConnectionEvent(Client client, boolean connect) {
            this.client = client;
            this.connect = connect;
        }
    }
}
