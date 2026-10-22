package com.securebank.card;

import com.securebank.common.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    public record FreezeRequest(@NotNull Boolean frozen) {}

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping
    public List<CardResponse> mine(@RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user) {
        return cardService.listMine(user);
    }

    @PatchMapping("/{id}/freeze")
    public CardResponse freeze(@RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user,
                               @PathVariable Long id, @Valid @RequestBody FreezeRequest request) {
        return cardService.setFrozen(user, id, request.frozen());
    }
}
