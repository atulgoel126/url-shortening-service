package com.linksplit.controller;

import com.linksplit.entity.Referrer;
import com.linksplit.service.ReferrerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/referrers")
@RequiredArgsConstructor
public class AdminReferrerController {

    private final ReferrerService referrerService;

    @GetMapping
    public String listReferrers(Model model) {
        model.addAttribute("referrers", referrerService.findAll());
        return "admin/referrers/list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("referrer", new Referrer());
        return "admin/referrers/add";
    }

    @PostMapping
    public String addReferrer(@ModelAttribute Referrer referrer) {
        referrerService.save(referrer);
        return "redirect:/admin/referrers";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Referrer referrer = referrerService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid referrer Id:" + id));
        model.addAttribute("referrer", referrer);
        return "admin/referrers/edit";
    }

    @PostMapping("/{id}")
    public String updateReferrer(@PathVariable Long id, @ModelAttribute Referrer referrer) {
        referrer.setId(id);
        referrerService.save(referrer);
        return "redirect:/admin/referrers";
    }

    @GetMapping("/{id}/delete")
    public String deleteReferrer(@PathVariable Long id) {
        referrerService.deleteById(id);
        return "redirect:/admin/referrers";
    }

    @GetMapping("/{id}/analytics")
    public String showAnalytics(@PathVariable Long id, Model model) {
        Referrer referrer = referrerService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid referrer Id:" + id));
        model.addAttribute("referrer", referrer);
        model.addAttribute("revenueByUser", referrerService.getRevenueByUser(referrer));
        return "admin/referrers/analytics";
    }
}