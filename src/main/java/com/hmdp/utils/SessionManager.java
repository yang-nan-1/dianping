package com.hmdp.utils;

import com.hmdp.dto.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionManager {

    private final Map<String, List<Message>> sessions = new ConcurrentHashMap<>();

    public List<Message> getHistory(String sessionId) {
        return sessions.computeIfAbsent(sessionId, k -> new ArrayList<>());
    }

    public void addMessage(String sessionId, Message message) {
        getHistory(sessionId).add(message);
    }
}
