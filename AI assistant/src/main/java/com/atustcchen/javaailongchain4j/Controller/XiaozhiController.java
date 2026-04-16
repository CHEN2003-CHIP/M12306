package com.atustcchen.javaailongchain4j.Controller;

import com.atustcchen.javaailongchain4j.Bean.ChatForm;
import com.atustcchen.javaailongchain4j.assistant.XiaozhiAgent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Tag(name="USTC小窝")
@RestController
public class XiaozhiController {
    @Autowired
    private XiaozhiAgent xiaozhiAgent;

    @Operation(summary="12306客服")
    @PostMapping(value="/chat" , produces = "text/stream;charset=utf-8")
    public Flux<String> chat(@RequestBody ChatForm chatForm)
    {
        return xiaozhiAgent.chat(chatForm.getMemoryId(), chatForm.getUserMessage())
                // 关键：添加日志，观察每个片段的输出时机
                .doOnNext(segment -> System.out.println("Flux发射片段：" + segment));

    }
}
