package com.securebank.card;

import com.securebank.audit.AuditService;
import com.securebank.auth.UserRepository;
import com.securebank.common.ApiException;
import com.securebank.common.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class CardService {

    private static final String BIN = "4821";
    private final SecureRandom random = new SecureRandom();

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final Clock clock;

    public CardService(CardRepository cardRepository, UserRepository userRepository,
                       AuditService auditService, Clock clock) {
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    /** Called when an account is opened. */
    @Transactional
    public Card issueFor(Long userId, String accountNumber) {
        String holder = userRepository.findById(userId)
                .map(u -> u.getName().toUpperCase(Locale.ROOT))
                .orElse("CARD HOLDER");
        String lastFour = String.format("%04d", random.nextInt(10_000));
        String expiry = LocalDate.now(clock).plusYears(5).format(DateTimeFormatter.ofPattern("MM/yy"));
        return cardRepository.save(new Card(userId, accountNumber, BIN, lastFour, holder, expiry));
    }

    @Transactional(readOnly = true)
    public List<CardResponse> listMine(CurrentUser user) {
        return cardRepository.findByUserIdOrderByIdAsc(user.id()).stream().map(CardResponse::from).toList();
    }

    @Transactional
    public CardResponse setFrozen(CurrentUser user, Long cardId, boolean frozen) {
        Card card = cardRepository.findById(cardId)
                .filter(c -> c.getUserId().equals(user.id()))
                .orElseThrow(() -> ApiException.notFound("Card not found"));
        card.setFrozen(frozen);
        auditService.log(user.id(), frozen ? "CARD_FROZEN" : "CARD_UNFROZEN", "Card", card.getId(),
                "Card ending " + card.getLastFour());
        return CardResponse.from(card);
    }
}
