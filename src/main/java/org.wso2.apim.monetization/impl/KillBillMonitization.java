package org.wso2.apim.monetization.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.killbill.billing.catalog.api.BillingPeriod;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.catalog.api.ProductCategory;
import org.killbill.billing.catalog.api.TimeUnit;
import org.killbill.billing.client.KillBillClientException;
import org.killbill.billing.client.KillBillHttpClient;
import org.killbill.billing.client.RequestOptions;
import org.killbill.billing.client.api.gen.CatalogApi;
import org.killbill.billing.client.model.Catalogs;
import org.killbill.billing.client.model.gen.SimplePlan;
import org.wso2.apim.monetization.impl.model.ApiConstantsKillBill;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.APIProvider;
import org.wso2.carbon.apimgt.api.MonetizationException;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.api.model.Monetization;
import org.wso2.carbon.apimgt.api.model.MonetizationUsagePublishInfo;
import org.wso2.carbon.apimgt.api.model.policy.SubscriptionPolicy;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class KillBillMonitization implements Monetization {
    private static final Log log = LogFactory.getLog(KillBillMonitization.class);



    /**
     * Create billing plan for a policy
     *
     * @param subscriptionPolicy subscription policy
     * @return true if successful, false otherwise
     * @throws MonetizationException if the action failed
     */
    @Override
    public boolean createBillingPlan(SubscriptionPolicy subscriptionPolicy) throws MonetizationException {
        try {
            // Read tenant conf and get platform account key
            KillBill.username = getUsername(subscriptionPolicy.getTenantDomain());
            log.error("\"Username\": \"" + KillBill.username + "\"");

            KillBill.password = getPassword(subscriptionPolicy.getTenantDomain());
            log.error("Password: " + KillBill.password);

            KillBill.apiKey = getApiKey(subscriptionPolicy.getTenantDomain());
            log.error("API Key: " + KillBill.apiKey);

            KillBill.apiSecret = getApiSecret(subscriptionPolicy.getTenantDomain());
            log.error("API Secret: " + KillBill.apiSecret);

            KillBill.createdBy = getCreatedBy(subscriptionPolicy.getTenantDomain());
            log.error("Created By: " + KillBill.createdBy);
        } catch (KillBillMonetizationException e) {
            String errorMessage = "Failed to get KillBill platform details for tenant domain: " +
                    subscriptionPolicy.getTenantDomain();
            throw new MonetizationException(errorMessage, e);
        }
        RequestOptions requestOptions = RequestOptions.builder()
                .withUser("admin")
                .withPassword("password")
                .withCreatedBy("inetum")
                .withTenantApiKey("inetum")
                .withTenantApiSecret("inetum")
                .build();

        Map<String, Object> planParams = new HashMap<String, Object>();
        planParams.put(ApiConstantsKillBill.POLICY_Id, subscriptionPolicy.getTenantDomain() +
                "-" + subscriptionPolicy.getPolicyId());
        planParams.put(ApiConstantsKillBill.PRODUCT_NAME, subscriptionPolicy.getTenantDomain() +
                "-" + subscriptionPolicy.getPolicyName());
        planParams.put(ApiConstantsKillBill.PRODUCT_CATEGORY, "BASE");
        String currencyType = subscriptionPolicy.getMonetizationPlanProperties().
                get(APIConstants.Monetization.CURRENCY).toLowerCase();
        planParams.put(ApiConstantsKillBill.CURRENCY, currencyType);
        float amount = Float.parseFloat(subscriptionPolicy.getMonetizationPlanProperties().
                get(APIConstants.Monetization.FIXED_PRICE));
        planParams.put(ApiConstantsKillBill.AMOUNT, (int) (amount * 100));
        String Cycle = subscriptionPolicy.getMonetizationPlanProperties().get(APIConstants.Monetization.BILLING_CYCLE);

        planParams.put(ApiConstantsKillBill.BILLING_PERIOD, getPlan(Cycle));

        planParams.put(ApiConstantsKillBill.TRIAL_LENGHT, 0);
        planParams.put(ApiConstantsKillBill.TRIAL_TIME_UNIT, "DAYS");
        planParams.put(ApiConstantsKillBill.AVAILABLE_BASE_PRODUCTS, "TEST");



        SimplePlan body = new SimplePlan(
                subscriptionPolicy.getTenantDomain() + "-" + subscriptionPolicy.getPolicyId(),
                subscriptionPolicy.getTenantDomain() + "-" + subscriptionPolicy.getPolicyName(),
                ProductCategory.BASE,
                Currency.USD,
                BigDecimal.valueOf(amount),
                getPlan(Cycle),
                0,
                TimeUnit.DAYS,
                Collections.singletonList("TEST")
        );
        String requestBody = "{" +
                "\"planId\": \""+ subscriptionPolicy.getTenantDomain() +
                "-" + subscriptionPolicy.getPolicyName()+ "\"," +
                "\"productName\": \"" +subscriptionPolicy.getTenantDomain() + "-" + subscriptionPolicy.getPolicyName()+ "\"," +
                "\"productCategory\": \"" +ProductCategory.BASE + "\"," +
                "\"currency\": \"" + subscriptionPolicy.getMonetizationPlanProperties().
                get(APIConstants.Monetization.CURRENCY).toLowerCase() + "\"," +
                "\"amount\": " + BigDecimal.valueOf(amount) + "," +
                "\"billingPeriod\": \"" +  getPlan(Cycle) + "\"," +
                "\"trialLength\": " + 0 + "," +
                "\"trialTimeUnit\": \"" + TimeUnit.DAYS + "\"," +
                "\"availableBaseProducts\": []" + // Assuming availableBaseProducts is an empty array
                "}";


        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost("http://localhost:8080/1.0/kb/catalog/simplePlan");

            // Set headers
            httpPost.setHeader("X-Killbill-ApiSecret", "inetum");
            httpPost.setHeader("X-Killbill-ApiKey", "inetum");
            httpPost.setHeader("X-Killbill-CreatedBy","inetum");
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString(("admin" + ":" +"password").getBytes()));

            // Set request body
            StringEntity requestEntity = new StringEntity(requestBody);
            httpPost.setEntity(requestEntity);



            String createdPlanId = "plan_" + subscriptionPolicy.getPolicyName();
            String productId ="prod_" + subscriptionPolicy.getTenantDomain() + "-" + subscriptionPolicy.getPolicyName();

            // Execute the request
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());

                if (statusCode >= 200 && statusCode < 300) {
                    log.error("Successfully added plan: ");
                    log.error("Response: " + responseBody);
                    //add database record
                    //add database record
                    KillBillDAO.addMonetizationPlanData(subscriptionPolicy, productId, createdPlanId);
                    return true;
                } else {
                    log.error("Failed to add plan. Status code: " + statusCode);
                    log.error("Response: " + responseBody);
                    return false;
                }
            } catch (KillBillMonetizationException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to execute HTTP request", e);
        }
    }



    @Override
    public boolean updateBillingPlan(SubscriptionPolicy subscriptionPolicy) throws MonetizationException {
        return false;
    }

    @Override
    public boolean deleteBillingPlan(SubscriptionPolicy subscriptionPolicy) throws MonetizationException {
        return false;
    }

    @Override
    public boolean enableMonetization(String s, API api, Map<String, String> map) throws MonetizationException {
        return false;
    }

    @Override
    public boolean disableMonetization(String s, API api, Map<String, String> map) throws MonetizationException {
        return false;
    }

    @Override
    public Map<String, String> getMonetizedPoliciesToPlanMapping(API api) throws MonetizationException {
        return null;
    }

    @Override
    public Map<String, String> getCurrentUsageForSubscription(String s, APIProvider apiProvider) throws MonetizationException {
        return null;
    }

    @Override
    public Map<String, String> getTotalRevenue(API api, APIProvider apiProvider) throws MonetizationException {
        return null;
    }

    @Override
    public boolean publishMonetizationUsageRecords(MonetizationUsagePublishInfo monetizationUsagePublishInfo) throws MonetizationException {
        return false;
    }

    private String getUsername(String Authorize) throws KillBillMonetizationException {

        try {
            JSONObject tenantConfig = APIUtil.getTenantConfig(Authorize);
            if (tenantConfig.containsKey(KillBillMonetizationConstants.MONETIZATION_INFO)) {
                JSONObject monetizationInfo = (JSONObject) tenantConfig
                        .get(KillBillMonetizationConstants.MONETIZATION_INFO);
                if (monetizationInfo.containsKey(KillBillMonetizationConstants.BILLING_ENGINE_PLATFORM_USERNAME)) {
                    String UserNameKillBill = monetizationInfo
                            .get(KillBillMonetizationConstants.BILLING_ENGINE_PLATFORM_USERNAME).toString();
                    if (StringUtils.isBlank(UserNameKillBill)) {
                        String errorMessage = "username platform account key is empty for tenant : " + Authorize;
                        throw new KillBillMonetizationException(errorMessage);
                    }
                    return UserNameKillBill;
                }
            }
        } catch (APIManagementException e) {
            String errorMessage = "Failed to get the configuration for tenant from DB:  " + Authorize;
            log.error(errorMessage);
            throw new KillBillMonetizationException(errorMessage, e);
        }
        return StringUtils.EMPTY;
    }

    private String getPassword(String Authorize) throws KillBillMonetizationException {

        try {
            JSONObject tenantConfig = APIUtil.getTenantConfig(Authorize);
            if (tenantConfig.containsKey(KillBillMonetizationConstants.MONETIZATION_INFO)) {
                JSONObject monetizationInfo = (JSONObject) tenantConfig
                        .get(KillBillMonetizationConstants.MONETIZATION_INFO);
                if (monetizationInfo.containsKey(KillBillMonetizationConstants.BILLING_ENGINE_PLATFORM_PASSWORD)) {
                    String passwordKillBill = monetizationInfo
                            .get(KillBillMonetizationConstants.BILLING_ENGINE_PLATFORM_PASSWORD).toString();
                    if (StringUtils.isBlank(passwordKillBill)) {
                        String errorMessage = "password platform account key is empty for tenant : " + Authorize;
                        throw new KillBillMonetizationException(errorMessage);
                    }
                    return passwordKillBill;
                }
            }
        } catch (APIManagementException e) {
            String errorMessage = "Failed to get the configuration for tenant from DB:  " + Authorize;
            log.error(errorMessage);
            throw new KillBillMonetizationException(errorMessage, e);
        }
        return StringUtils.EMPTY;
    }

    private String getApiKey(String Authorize) throws KillBillMonetizationException {

        try {
            JSONObject tenantConfig = APIUtil.getTenantConfig(Authorize);
            if (tenantConfig.containsKey(KillBillMonetizationConstants.MONETIZATION_INFO)) {
                JSONObject monetizationInfo = (JSONObject) tenantConfig
                        .get(KillBillMonetizationConstants.MONETIZATION_INFO);

                if (monetizationInfo.containsKey(KillBillMonetizationConstants.BILLING_ENGINE_PLATFORM_API_KEY)) {
                    String apiKeyKillBill = monetizationInfo
                            .get(KillBillMonetizationConstants.BILLING_ENGINE_PLATFORM_API_KEY).toString();
                    if (StringUtils.isBlank(apiKeyKillBill)) {
                        String errorMessage = "apiKey platform account key is empty for tenant : " + Authorize;
                        throw new KillBillMonetizationException(errorMessage);
                    }
                    return apiKeyKillBill;
                }

            }
        } catch (APIManagementException e) {
            String errorMessage = "Failed to get the configuration for tenant from DB:  " + Authorize;
            log.error(errorMessage);
            throw new KillBillMonetizationException(errorMessage, e);
        }
        return StringUtils.EMPTY;
    }

    private String getApiSecret(String Authorize) throws KillBillMonetizationException {

        try {
            JSONObject tenantConfig = APIUtil.getTenantConfig(Authorize);
            if (tenantConfig.containsKey(KillBillMonetizationConstants.MONETIZATION_INFO)) {
                JSONObject monetizationInfo = (JSONObject) tenantConfig
                        .get(KillBillMonetizationConstants.MONETIZATION_INFO);
                if (monetizationInfo.containsKey(KillBillMonetizationConstants.BILLING_ENGINE_PLATFORM_API_SECRET)) {
                    String apiSecretKillBill = monetizationInfo
                            .get(KillBillMonetizationConstants.BILLING_ENGINE_PLATFORM_API_SECRET).toString();
                    if (StringUtils.isBlank(apiSecretKillBill)) {
                        String errorMessage = "apiSecret platform account key is empty for tenant : " + Authorize;
                        throw new KillBillMonetizationException(errorMessage);
                    }
                    return apiSecretKillBill;
                }

            }
        } catch (APIManagementException e) {
            String errorMessage = "Failed to get the configuration for tenant from DB:  " + Authorize;
            log.error(errorMessage);
            throw new KillBillMonetizationException(errorMessage, e);
        }
        return StringUtils.EMPTY;
    }

    private String getCreatedBy(String Authorize) throws KillBillMonetizationException {

        try {
            JSONObject tenantConfig = APIUtil.getTenantConfig(Authorize);
            if (tenantConfig.containsKey(KillBillMonetizationConstants.MONETIZATION_INFO)) {
                JSONObject monetizationInfo = (JSONObject) tenantConfig
                        .get(KillBillMonetizationConstants.MONETIZATION_INFO);
                if (monetizationInfo.containsKey(KillBillMonetizationConstants.BILLING_ENGINE_PLATFORM_CREATED_BY)) {
                    String CreatedByKillBill = monetizationInfo
                            .get(KillBillMonetizationConstants.BILLING_ENGINE_PLATFORM_CREATED_BY).toString();
                    if (StringUtils.isBlank(CreatedByKillBill)) {
                        String errorMessage = "username platform account key is empty for tenant : " + Authorize;
                        throw new KillBillMonetizationException(errorMessage);
                    }
                    return CreatedByKillBill;
                }
            }
        } catch (APIManagementException e) {
            String errorMessage = "Failed to get the configuration for tenant from DB:  " + Authorize;
            log.error(errorMessage);
            throw new KillBillMonetizationException(errorMessage, e);
        }
        return StringUtils.EMPTY;
    }

    private BillingPeriod getPlan(String paln){
        switch (paln) {
                case "Week":
                    return BillingPeriod.WEEKLY;
                case "Month":
                    return BillingPeriod.MONTHLY;
                default:
                    return BillingPeriod.ANNUAL;
            }
    }
   /* public enum Currency {
        USD,
        EUR,
        // Ajoutez d'autres devises supportées ici
    }

    private Currency mapCurrency(String currencyType) {
        // Supposons que vous avez une liste prédéfinie des devises prises en charge dans votre application
        // et que vous voulez mapper les noms des devises aux instances de l'enum Currency.
        // Par exemple :
        switch (currencyType.toUpperCase()) {
            case "USD":
                return Currency.USD;
            case "EUR":
                return Currency.EUR;
            // Ajoutez d'autres cas pour d'autres devises supportées
            default:
                throw new IllegalArgumentException("Currency type not supported: " + currencyType);
        }
    }
*/

}
