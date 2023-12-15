package com.roidmc.coreutil;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.roidmc.coreutil.websocket.WSMessage;
import com.roidmc.coreutil.websocket.WebSocket;
import com.roidmc.coreutil.websocket.WSChannel;
import com.roidmc.coreutil.websocket.WebSocketServer;

public class Main {
    public static void main(String[] args) {
        testServer();
        testClient();
    }

    public static void testClient(){
        Gson gson = new Gson();
        WebSocket webSocket = new WebSocket("127.0.0.1",3332);
        WSChannel channel = webSocket.findChannel("teste");

        channel.subscribe((message)->{
            System.out.println("Server message: \n"+message.getBody());
        });

        WSMessage request = channel.createMessage("Plugin 1");
        request.setBody("[Client] Perguntei").send(()->{
            WSMessage reply = request.getReply().reply("Plugin 1");
            reply.setBody(new JsonPrimitive("[Client] Respondendo a sua resposta"), request.getReply().getBody()).send();
        });
    }

    public static void testServer(){
        WebSocketServer server = new WebSocketServer(3332);
        WSChannel channel = server.findChannel("teste");

        channel.subscribe((message)->{
            System.out.println("Client message: \n"+message.getBody());
            if(message.getReplyTo()!=null)return;
            WSMessage reply = message.reply("Server 1");
            reply.setBody(new JsonPrimitive("[Server] Respondendo a sua pergunta"),message.getBody()).send(()->{
                reply.getReply().reply("Server 1").setBody(new JsonPrimitive("[Server] Respondendo a sua resposta sobre a minha resposta da sua pergunta"),reply.getReply().getBody()).send();
            });
        });


    }
}