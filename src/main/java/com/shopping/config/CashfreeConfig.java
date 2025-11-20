// src/main/java/com/shopping/config/CashfreeConfig.java
package com.shopping.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
@Component
@ConfigurationProperties(prefix = "cashfree")
@Data
public class CashfreeConfig {
    private String appId;
    private String secretKey;
    private String baseUrl;
    private String mode = "PROD";
    private String returnUrl; 
    private String merchantUpiId;
    
    public String getAppId() {
        return appId != null ? appId.trim() : null;
    }

    public String getSecretKey() {
        return secretKey != null ? secretKey.trim() : null;
    }

    public String getBaseUrl() {
        return baseUrl != null ? baseUrl.trim() : null;
    }

    public String getMerchantUpiId() {
        return merchantUpiId != null ? merchantUpiId.trim() : null;
    }

}