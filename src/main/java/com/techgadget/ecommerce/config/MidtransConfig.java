package com.techgadget.ecommerce.config;

import com.midtrans.Config;
import com.midtrans.Midtrans;
import com.midtrans.service.MidtransCoreApi;
import com.midtrans.service.MidtransSnapApi;
import com.midtrans.service.impl.MidtransCoreApiImpl;
import com.midtrans.service.impl.MidtransSnapApiImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MidtransConfig {

    @Value("${midtrans.server-key}")
    private String serverKey;

    @Value("${midtrans.client-key}")
    private String clientKey;

    @Value("${midtrans.is-production}")
    private boolean isProduction;

    public Config midtransConfig() {
        return Config.builder()
                .setServerKey(serverKey)
                .setClientKey(clientKey)
                .setIsProduction(isProduction)
                .build();
    }

    // ---------------------
    // Using midtrans API with bean object
    // So can be mocked at testing easily
    // ---------------------

    @Bean
    public MidtransCoreApi midtransCoreApi() {
        return new MidtransCoreApiImpl(midtransConfig());
    }

    @Bean
    public MidtransSnapApi midtransSnapApi() {
        return new MidtransSnapApiImpl(midtransConfig());
    }

}
