package com.pabloncf.saas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "stripe")
public class StripeConfig {

    private String secretKey;
    private String webhookSecret;
    private PriceIds priceIds = new PriceIds();

    public static class PriceIds {
        private String pro;
        private String enterprise;

        public String getPro()              { return pro;        }
        public void   setPro(String pro)    { this.pro = pro;    }
        public String getEnterprise()                    { return enterprise;             }
        public void   setEnterprise(String enterprise)   { this.enterprise = enterprise;  }
    }

    public String    getSecretKey()                        { return secretKey;              }
    public void      setSecretKey(String secretKey)        { this.secretKey = secretKey;    }
    public String    getWebhookSecret()                    { return webhookSecret;          }
    public void      setWebhookSecret(String secret)       { this.webhookSecret = secret;   }
    public PriceIds  getPriceIds()                         { return priceIds;               }
    public void      setPriceIds(PriceIds priceIds)        { this.priceIds = priceIds;      }
}
