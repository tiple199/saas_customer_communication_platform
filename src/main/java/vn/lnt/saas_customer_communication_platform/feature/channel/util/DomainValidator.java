package vn.lnt.saas_customer_communication_platform.feature.channel.util;

import vn.lnt.saas_customer_communication_platform.exception.InvalidOperationException;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Tiện ích kiểm tra định dạng và xác thực nguồn gốc (Origin/Domain) cho Web Channel Widget.
 * Áp dụng quy chuẩn nhập liệu nghiêm ngặt (Strict Form) để tối ưu xử lý và nhất quán dữ liệu.
 */
public final class DomainValidator {

    // Regex kiểm tra tên miền hoặc localhost với cổng tùy chọn, hỗ trợ wildcard *.
    // Ví dụ hợp lệ: example.com, sub.example.com, *.example.com, localhost, localhost:3000, domain-123.vn:8080
    private static final Pattern DOMAIN_PATTERN = Pattern.compile(
            "^(\\*\\.)?([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)*[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?(:[0-9]{1,5})?$"
    );

    private DomainValidator() {
    }

    /**
     * Kiểm tra và chuẩn hóa chuỗi allowedDomains theo quy chuẩn form nghiêm ngặt:
     * 1. Phân tách danh sách tên miền bằng dấu phẩy (,).
     * 2. Chỉ chấp nhận tên miền/hostname thuần túy (vd: example.com, *.example.com, localhost:3000).
     * 3. KHÔNG được chứa giao thức (http://, https://) hoặc đường dẫn (/).
     *
     * @param allowedDomains Chuỗi tên miền cấu hình từ client
     * @return Chuỗi tên miền chuẩn hóa (chữ thường, loại bỏ khoảng trắng dư thừa, lọc trùng lặp)
     * @throws InvalidOperationException nếu vi phạm quy chuẩn
     */
    public static String validateAndNormalize(String allowedDomains) {
        if (allowedDomains == null || allowedDomains.isBlank()) {
            return null;
        }

        if (allowedDomains.length() > 500) {
            throw new InvalidOperationException("Danh sách tên miền cho phép không được vượt quá 500 ký tự");
        }

        String[] tokens = allowedDomains.split(",");
        Set<String> normalizedSet = new LinkedHashSet<>();

        for (String rawToken : tokens) {
            String token = rawToken.trim();
            if (token.isEmpty()) {
                continue;
            }

            // Bắt buộc người dùng không nhập http://, https:// hoặc dấu /
            if (token.startsWith("http://") || token.startsWith("https://") || token.contains("/")) {
                throw new InvalidOperationException(
                        "Không nhập giao thức (http/https) hoặc đường dẫn (/): '" + token + "'. Vui lòng chỉ nhập tên miền (vd: example.com, *.example.com, localhost:3000)"
                );
            }

            if (!isValidDomainPattern(token)) {
                throw new InvalidOperationException(
                        "Tên miền không hợp lệ: '" + token + "'. Định dạng hợp lệ: example.com, *.example.com, localhost:3000"
                );
            }

            normalizedSet.add(token.toLowerCase(Locale.ROOT));
        }

        return String.join(",", normalizedSet);
    }

    /**
     * Kiểm tra xem Origin/Referer từ trình duyệt có khớp với allowedDomains đã cấu hình hay không.
     *
     * @param requestOrigin  Origin từ request trình duyệt (vd: "https://shop.example.com")
     * @param allowedDomains Chuỗi tên miền cho phép đã được chuẩn hóa trong Channel
     * @return true nếu hợp lệ hoặc nếu không cấu hình giới hạn (allow all)
     */
    public static boolean isOriginAllowed(String requestOrigin, String allowedDomains) {
        // Nếu không cấu hình hoặc cấu hình "*", cho phép tất cả các trang web
        if (allowedDomains == null || allowedDomains.isBlank() || allowedDomains.trim().equals("*")) {
            return true;
        }

        if (requestOrigin == null || requestOrigin.isBlank()) {
            return false;
        }

        String originHost = extractOriginHost(requestOrigin.trim()).toLowerCase(Locale.ROOT);
        String originHostOnly = stripPort(originHost);

        String[] allowedList = allowedDomains.split(",");
        for (String rawPattern : allowedList) {
            String pattern = rawPattern.trim().toLowerCase(Locale.ROOT);
            if (pattern.isEmpty()) {
                continue;
            }

            if (pattern.equals("*")) {
                return true;
            }

            // Trường hợp Wildcard: *.example.com
            if (pattern.startsWith("*.")) {
                String baseDomain = pattern.substring(2);
                String patternHostOnly = stripPort(baseDomain);
                if (originHostOnly.equals(patternHostOnly) || originHostOnly.endsWith("." + patternHostOnly)) {
                    return true;
                }
            } else {
                // Khớp chính xác (hỗ trợ cả có port hoặc không có port)
                if (pattern.contains(":")) {
                    if (originHost.equals(pattern)) {
                        return true;
                    }
                } else {
                    if (originHostOnly.equals(pattern) || originHost.equals(pattern)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean isValidDomainPattern(String domain) {
        if (domain == null || domain.isBlank()) {
            return false;
        }
        if (domain.equals("*")) {
            return true;
        }
        return DOMAIN_PATTERN.matcher(domain).matches();
    }

    /**
     * Bóc tách host và port từ header Origin/Referer của HTTP request từ trình duyệt.
     */
    private static String extractOriginHost(String requestOrigin) {
        try {
            URI uri = URI.create(requestOrigin);
            String host = uri.getHost();
            int port = uri.getPort();
            if (host != null) {
                return port != -1 ? host + ":" + port : host;
            }
        } catch (Exception ignored) {
        }

        String cleaned = requestOrigin.replaceFirst("^https?://", "");
        int slashIdx = cleaned.indexOf('/');
        if (slashIdx != -1) {
            cleaned = cleaned.substring(0, slashIdx);
        }
        return cleaned;
    }

    private static String stripPort(String hostAndPort) {
        int colonIdx = hostAndPort.indexOf(':');
        return colonIdx != -1 ? hostAndPort.substring(0, colonIdx) : hostAndPort;
    }
}
