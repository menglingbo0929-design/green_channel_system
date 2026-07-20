package com.example.backend.approval.web;

import com.example.backend.approval.api.SystemMessagePage;
import com.example.backend.approval.api.SystemMessageService;
import com.example.backend.approval.port.CurrentUserProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class SystemMessageController {

    private final ObjectProvider<CurrentUserProvider> currentUserProvider;
    private final ObjectProvider<SystemMessageService> messageServiceProvider;

    public SystemMessageController(
            ObjectProvider<CurrentUserProvider> currentUserProvider,
            ObjectProvider<SystemMessageService> messageServiceProvider
    ) {
        this.currentUserProvider = currentUserProvider;
        this.messageServiceProvider = messageServiceProvider;
    }

    @GetMapping
    public SystemMessagePage list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean read
    ) {
        return messages().listMessages(currentUser().userId(), page, size, read);
    }

    @PostMapping("/{messageId}/read")
    public void markRead(@PathVariable Long messageId, @Valid @RequestBody MarkReadRequest request) {
        messages().markAsRead(messageId, currentUser().userId());
    }

    private com.example.backend.approval.port.LoginUser currentUser() {
        CurrentUserProvider provider = currentUserProvider.getIfAvailable();
        if (provider == null) throw new ApprovalIntegrationUnavailableException("成员一 CurrentUserProvider");
        return provider.getRequiredUser();
    }

    private SystemMessageService messages() {
        SystemMessageService service = messageServiceProvider.getIfAvailable();
        if (service == null) throw new ApprovalIntegrationUnavailableException("成员三系统消息 Service");
        return service;
    }

    public record MarkReadRequest(@NotBlank String requestId) {
    }
}
