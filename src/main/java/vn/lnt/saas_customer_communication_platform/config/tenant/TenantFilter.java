package vn.lnt.saas_customer_communication_platform.config.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.lnt.saas_customer_communication_platform.feature.auth.entity.User;
import vn.lnt.saas_customer_communication_platform.feature.auth.repository.UserRepository;
import vn.lnt.saas_customer_communication_platform.feature.company.security.CompanySecurityEvaluator;

import java.io.IOException;

@Component
public class TenantFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final CompanySecurityEvaluator companySecurityEvaluator;

    public TenantFilter(UserRepository userRepository, CompanySecurityEvaluator companySecurityEvaluator) {
        this.userRepository = userRepository;
        this.companySecurityEvaluator = companySecurityEvaluator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            Long companyId = resolveCompanyId();
            if (companyId != null) {
                TenantContext.setTenantId(companyId);
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private Long resolveCompanyId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            String email = jwt.getSubject();
            if (email != null) {
                User user = userRepository.findByEmail(email).orElse(null);
                if (user != null && user.getCurrentCompanyId() != null) {
                    Long companyId = user.getCurrentCompanyId();
                    boolean isMember = companySecurityEvaluator.isMember(user.getId(), companyId);
                    if (isMember) {
                        return companyId;
                    }
                }
            }
        }

        return null;
    }
}
