package com.securebank.insights;

import com.securebank.common.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InsightsController {

    private final InsightsService insightsService;

    public InsightsController(InsightsService insightsService) {
        this.insightsService = insightsService;
    }

    @GetMapping("/api/insights/overview")
    public InsightsService.Overview overview(@RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user) {
        return insightsService.overview(user);
    }
}
