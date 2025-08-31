package com.linksplit.controller;

import com.linksplit.entity.PaymentMethod;
import com.linksplit.entity.Payout;
import com.linksplit.entity.User;
import com.linksplit.repository.PayoutRepository;
import com.linksplit.service.PaymentService;
import com.linksplit.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AccountController {
    private final UserService userService;
    private final PaymentService paymentService;
    private final PayoutRepository payoutRepository;
    
    @GetMapping("/account")
    public String showAccount(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        
        User user = userService.getUserByEmail(authentication.getName());
        List<PaymentMethod> paymentMethods = paymentService.getUserPaymentMethods(user);
        BigDecimal availableBalance = paymentService.getAvailableBalance(user);
        
        model.addAttribute("user", user);
        model.addAttribute("paymentMethods", paymentMethods);
        model.addAttribute("availableBalance", availableBalance);
        
        return "account";
    }
    
    @PostMapping("/account/upi/add")
    public String addUpiId(@RequestParam String upiId,
                          @RequestParam String accountHolderName,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        
        try {
            User user = userService.getUserByEmail(authentication.getName());
            
            // Basic UPI ID validation
            if (!upiId.matches("^[a-zA-Z0-9._-]+@[a-zA-Z0-9]+$")) {
                redirectAttributes.addFlashAttribute("error", "Invalid UPI ID format");
                return "redirect:/account";
            }
            
            paymentService.addUpiId(user, upiId, accountHolderName);
            redirectAttributes.addFlashAttribute("success", "UPI ID added successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/account";
    }
    
    @PostMapping("/account/payment-method/{id}/primary")
    public String setPrimaryPaymentMethod(@PathVariable Long id,
                                         Authentication authentication,
                                         RedirectAttributes redirectAttributes) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        
        try {
            User user = userService.getUserByEmail(authentication.getName());
            paymentService.setPrimaryPaymentMethod(user, id);
            redirectAttributes.addFlashAttribute("success", "Primary payment method updated");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/account";
    }
    
    @PostMapping("/account/payment-method/{id}/delete")
    public String deletePaymentMethod(@PathVariable Long id,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        
        try {
            User user = userService.getUserByEmail(authentication.getName());
            paymentService.deletePaymentMethod(user, id);
            redirectAttributes.addFlashAttribute("success", "Payment method deleted");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/account";
    }
    
    @GetMapping("/payouts")
    public String showPayouts(Model model, Authentication authentication,
                             @RequestParam(defaultValue = "0") int page) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        
        User user = userService.getUserByEmail(authentication.getName());
        Page<Payout> payouts = payoutRepository.findByUserOrderByRequestedAtDesc(
                user, PageRequest.of(page, 10));
        
        BigDecimal totalPaid = payoutRepository.getTotalPayoutsByUserAndStatus(
                user, Payout.PayoutStatus.COMPLETED);
        BigDecimal pendingAmount = payoutRepository.getTotalPayoutsByUserAndStatus(
                user, Payout.PayoutStatus.PENDING);
        BigDecimal availableBalance = paymentService.getAvailableBalance(user);
        
        model.addAttribute("payouts", payouts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", payouts.getTotalPages());
        model.addAttribute("totalPaid", totalPaid != null ? totalPaid : BigDecimal.ZERO);
        model.addAttribute("pendingAmount", pendingAmount != null ? pendingAmount : BigDecimal.ZERO);
        model.addAttribute("availableBalance", availableBalance);
        
        return "payouts";
    }
    
    @PostMapping("/payouts/request")
    public String requestPayout(Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        
        try {
            User user = userService.getUserByEmail(authentication.getName());
            Payout payout = paymentService.requestPayout(user);
            redirectAttributes.addFlashAttribute("success", 
                    "Payout request created: " + payout.getReferenceNumber());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/payouts";
    }
    
    @PostMapping("/payouts/{id}/cancel")
    public String cancelPayout(@PathVariable Long id,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        
        try {
            User user = userService.getUserByEmail(authentication.getName());
            paymentService.cancelPayout(user, id);
            redirectAttributes.addFlashAttribute("success", "Payout cancelled successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/payouts";
    }
    
    @GetMapping("/api/account/balance")
    @ResponseBody
    public ResponseEntity<?> getBalance(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        
        User user = userService.getUserByEmail(authentication.getName());
        BigDecimal balance = paymentService.getAvailableBalance(user);
        
        return ResponseEntity.ok(Map.of("balance", balance));
    }
}