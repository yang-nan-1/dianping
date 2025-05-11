package com.hmdp.controller;

import com.hmdp.dto.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AIAssistantController {

    @PostMapping("/chat")
    public Result chat(@RequestBody Map<String, String> payload) {
        String question = payload.get("message");
        return Result.ok("这是回答");
    }

}
