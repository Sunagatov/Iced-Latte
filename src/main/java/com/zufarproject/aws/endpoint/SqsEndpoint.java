package com.zufarproject.aws.endpoint;

import com.amazonaws.services.sqs.model.Message;
import com.zufarproject.aws.sqs.SqsMessageReceiver;
import com.zufarproject.aws.sqs.SqsMessageSender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(value = "/sqs")
public class SqsEndpoint {
    private final SqsMessageSender sqsMessageSender;
    private final SqsMessageReceiver sqsMessageReceiver;

    @PostMapping
    @ResponseBody
    public ResponseEntity<Void> send(@RequestBody @Valid @NotNull(message = "messageBody is mandatory") final String messageBody) {
        sqsMessageSender.send(messageBody);
        return ResponseEntity.status(HttpStatus.OK)
                .build();
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Collection<Message>> getCustomerById() {
        Collection<Message> messages = sqsMessageReceiver.receive();
        return ResponseEntity.ok()
                .body(messages);
    }

}
