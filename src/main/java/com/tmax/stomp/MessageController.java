package com.tmax.stomp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.ArrayList;
@Slf4j
@RestController
@RequiredArgsConstructor
public class MessageController {

    private final SimpMessageSendingOperations sendingOperations;

    ArrayList<String> users = new ArrayList<String>();

    // 새로운 사용자가 웹 소켓을 연결할 때 실행됨
    // @EventListener은 한개의 매개변수만 가질 수 있다.
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        log.info("Received a new web socket connection");
    }

    // 사용자가 웹 소켓 연결을 끊으면 실행됨
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sender = (String) headerAccessor.getSessionAttributes().get("sender");
        String roomId = (String) headerAccessor.getSessionAttributes().get("roomId");

        if(sender != null) {
            log.info("User Disconnected : " + sender);

            users.remove(sender);

            ChatMessage message =ChatMessage.builder().type(ChatMessage.MessageType.LEAVE)
                    .roomId(roomId)
                    .sender(sender)
                    .message(sender+"님이 나갔습니다")
                    .build();
            sendingOperations.convertAndSend("/topic/chat/room/"+roomId, message);
        }
    }


    @MessageMapping("/chat/message")
    public void enter(@Payload ChatMessage message, SimpMessageHeaderAccessor headerAccessor) {
        if (ChatMessage.MessageType.ENTER.equals(message.getType())) {
            message.setMessage(message.getSender()+"님이 입장하였습니다.");
            headerAccessor.getSessionAttributes().put("sender", message.getSender());
            headerAccessor.getSessionAttributes().put("roomId", message.getRoomId());
            users.add(message.getSender());
        }
        log.info("Message Type : " + message.getType());
        sendingOperations.convertAndSend("/topic/chat/room/"+message.getRoomId(),message);
    }
}
