package com.securebank.assistant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AssistantController {

    public record AskRequest(@NotBlank @Size(max = 500) String question) {}

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/api/assistant/ask")
    public AssistantService.Answer ask(@Valid @RequestBody AskRequest request) {
        return assistantService.ask(request.question());
    }
}
