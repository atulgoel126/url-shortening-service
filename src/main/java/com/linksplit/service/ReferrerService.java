package com.linksplit.service;

import com.linksplit.entity.Referrer;
import com.linksplit.entity.User;
import com.linksplit.repository.ReferrerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReferrerService {

    private final ReferrerRepository referrerRepository;
    private final RevenueService revenueService;

    public List<Referrer> findAll() {
        return referrerRepository.findAllWithUsers();
    }

    public Optional<Referrer> findById(Long id) {
        return referrerRepository.findByIdWithUsers(id);
    }

    @Transactional
    public Referrer save(Referrer referrer) {
        return referrerRepository.save(referrer);
    }

    @Transactional
    public void deleteById(Long id) {
        referrerRepository.deleteById(id);
    }

    public Map<User, BigDecimal> getRevenueByUser(Referrer referrer) {
        Map<User, BigDecimal> revenueByUser = new HashMap<>();
        for (User user : referrer.getUsers()) {
            BigDecimal totalRevenue = user.getLinks().stream()
                    .map(link -> revenueService.calculateEarnings(link.getViewCount(), user))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            revenueByUser.put(user, totalRevenue);
        }
        return revenueByUser;
    }
}