package org.wso2.apim.monetization.impl;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
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
import org.wso2.carbon.apimgt.api.model.Tier;
import org.wso2.carbon.apimgt.api.model.policy.SubscriptionPolicy;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.dao.ApiMgtDAO;
import org.wso2.carbon.apimgt.impl.utils.APIMgtDBUtil;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.context.PrivilegedCarbonContext;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
            KillBill.username = getUsername(subscriptionPolicy.getTenantDomain());
            log.error("Username:" + KillBill.username);
            KillBill.password = getPassword(subscriptionPolicy.getTenantDomain());
            log.error("Password: " + KillBill.password);
            KillBill.apiKey = getApiKey(subscriptionPolicy.getTenantDomain());
            log.error("API Key: " + KillBill.apiKey);
            KillBill.apiSecret = getApiSecret(subscriptionPolicy.getTenantDomain());
            log.error("API Secret: " + KillBill.apiSecret);
            KillBill.createdBy = getCreatedBy(subscriptionPolicy.getTenantDomain());
            log.error("Created By: " + KillBill.createdBy);
        } catch (KillBillMonetizationException e) {
            String errorMessage = "Failed to get KillBill platform details for tenant domain: " + subscriptionPolicy.getTenantDomain();
            throw new MonetizationException(errorMessage, e);
        }
        RequestOptions requestOptions = RequestOptions.builder().withUser(KillBill.username).withPassword(KillBill.password).withCreatedBy(KillBill.createdBy).withTenantApiKey(KillBill.apiKey).withTenantApiSecret(KillBill.apiSecret).build();
        Map<String, Object> planParams = new HashMap<>();
        planParams.put("planId", subscriptionPolicy.getTenantDomain() + "-" + subscriptionPolicy
                .getPolicyId());
        planParams.put("productName", subscriptionPolicy.getTenantDomain() + "-" + subscriptionPolicy
                .getPolicyName());
        planParams.put("BASE", "BASE");
        String currencyType = ((String)subscriptionPolicy.getMonetizationPlanProperties().get("currencyType")).toLowerCase();
        planParams.put("currency", currencyType);
        float amount = Float.parseFloat((String)subscriptionPolicy.getMonetizationPlanProperties()
                .get("fixedPrice"));
        planParams.put("amount", Integer.valueOf((int)(amount * 100.0F)));
        String Cycle = subscriptionPolicy.getMonetizationPlanProperties().get("billingCycle");
        log.error("Cycle<<<<<<<<<<>: " +Cycle);;
        planParams.put("billingPeriod", getPlan(Cycle));
        planParams.put("trialLength", Integer.valueOf(0));
        planParams.put("trialTimeUnit", "DAYS");
        planParams.put("availableBaseProducts", "[]");
        log.error("planParamsssssssssssssssss>: " +planParams);
        SimplePlan body = new SimplePlan(subscriptionPolicy.getTenantDomain() + "-"  + subscriptionPolicy.getPolicyName(), subscriptionPolicy.getTenantDomain() + "-" + subscriptionPolicy.getPolicyName(), ProductCategory.BASE, Currency.USD, BigDecimal.valueOf(amount), getPlan(Cycle), Integer.valueOf(0), TimeUnit.DAYS, Collections.singletonList(""));
        log.error("body<<<<<<<<<<>: " +body);;
        String requestBody = "{" +
                "\"planId\":\"killBill-" + subscriptionPolicy.getPolicyName() +  "\"," +
                "\"productName\": \"" +subscriptionPolicy.getTenantDomain() + "-" + subscriptionPolicy.getPolicyName()+ "\"," +
                "\"productCategory\": \"" +ProductCategory.BASE + "\"," +
                "\"currency\": \"" + Currency.USD+ "\"," +
                "\"amount\": " + BigDecimal.valueOf(amount) + "," +
                "\"billingPeriod\": \"" +  getPlan(Cycle) + "\"," +
                "\"trialLength\": " + 0 + "," +
                "\"trialTimeUnit\": \"" + TimeUnit.DAYS + "\"," +
                "\"availableBaseProducts\": []" + // Assuming availableBaseProducts is an empty array
                "}";

        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            try {
                HttpPost httpPost = new HttpPost("http://localhost:8080/1.0/kb/catalog/simplePlan");
                httpPost.setHeader("X-Killbill-ApiSecret",  KillBill.apiSecret);
                httpPost.setHeader("X-Killbill-ApiKey",  KillBill.apiKey);
                httpPost.setHeader("X-Killbill-CreatedBy",  KillBill.createdBy);
                httpPost.setHeader("Content-Type", "application/json");
                httpPost.setHeader("Accept", "application/json");
                httpPost.setHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString((KillBill.username + ":" + KillBill.password).getBytes()));
                StringEntity requestEntity = new StringEntity(requestBody);
                httpPost.setEntity(requestEntity);
                String createdPlanId = "killBill-" + subscriptionPolicy.getPolicyName();
                String productId = subscriptionPolicy.getTenantDomain() + "-" + subscriptionPolicy.getPolicyName();
                try {
                    CloseableHttpResponse response = httpClient.execute((HttpUriRequest)httpPost);
                    try {
                        int statusCode = response.getStatusLine().getStatusCode();
                        String responseBody = EntityUtils.toString(response.getEntity());
                        if (statusCode >= 200 && statusCode < 300) {
                            log.error("Successfully added plan: ");
                            log.error("Response: " + responseBody);
                            KillBillDAO.addMonetizationPlanData(subscriptionPolicy, productId, createdPlanId);
                            boolean bool1 = true;
                            if (response != null)
                                response.close();
                            if (httpClient != null)
                                httpClient.close();
                            return bool1;
                        }
                        log.error("Failed to add plan. Status code: " + statusCode);
                        log.error("Response: " + responseBody);
                        boolean bool = false;
                        if (response != null)
                            response.close();
                        if (httpClient != null)
                            httpClient.close();
                        return bool;
                    } catch (Throwable throwable) {
                        if (response != null)
                            try {
                                response.close();
                            } catch (Throwable throwable1) {
                                throwable.addSuppressed(throwable1);
                            }
                        throw throwable;
                    }
                } catch (KillBillMonetizationException e) {
                    throw new RuntimeException(e);
                }
            } catch (Throwable throwable) {
                if (httpClient != null)
                    try {
                        httpClient.close();
                    } catch (Throwable throwable1) {
                        throwable.addSuppressed(throwable1);
                    }
                throw throwable;
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
    public boolean enableMonetization(String tenantDomain, API api, Map<String, String> monetizationProperties) throws MonetizationException {


            String apiName = api.getId().getApiName();
            String apiVersion = api.getId().getVersion();
            String apiProvider = api.getId().getProviderName();

        log.error("apiName: " + apiName);
            try (Connection con = APIMgtDBUtil.getConnection()) {
                int apiId = ApiMgtDAO.getInstance().getAPIID(api.getUuid(), con);
                String billingProductIdForApi = getBillingProductIdForApi(apiId);
                String getbillingPlanId = getBillingPlanId(apiId);
                log.error("getbillingPlanId :"+getbillingPlanId);
                Map<String, String> tierPlanMap = new HashMap<String, String>();
                //scan for commercial tiers and add plans in the billing engine if needed
                for (Tier currentTier : api.getAvailableTiers()) {
                    if (APIConstants.COMMERCIAL_TIER_PLAN.equalsIgnoreCase(currentTier.getTierPlan())) {
                        String billingPlanId = getBillingPlanIdOfTier(apiId, currentTier.getName());
                        log.error("billingPlanId: "+billingPlanId);
                        if (StringUtils.isBlank(billingPlanId)) {
                            int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
                            log.error("billingPlanId: "+billingPlanId);

                                tierPlanMap.put(currentTier.getName(),"killBill-"+ currentTier.getName());
                            log.error("tierPlanMap: "+tierPlanMap);
                            KillBillDAO.addMonetizationData(apiId, billingProductIdForApi, tierPlanMap);
                            log.error("addMonetizationData: "+tierPlanMap);

                        }
                    }
                }



                String requestBody = "{" +
                        "\"accountId\": \"" + "07ef8899-76d1-4fbd-85fb-1daac75a3eee" + "\"," +
                        "\"externalKey\": \"" + "07ef8899-76d1-4fbd-85fb-1daac75a3eee" + "\"," +
                        "\"planName\": \"killBill-" + getbillingPlanId + "\"" +
                        "}";

                try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                    HttpPost httpPost = new HttpPost("http://localhost:8080/1.0/kb/subscriptions");

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


                    // Execute the request
                    try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                        int statusCode = response.getStatusLine().getStatusCode();
                        String responseBody = EntityUtils.toString(response.getEntity());

                        if (statusCode >= 200 && statusCode < 300) {
                            log.error("Successfully Monetization plan: ");
                            log.error("Response: " + responseBody);
                            //add database record
                            //add database record

                            return true;
                        } else {
                            log.error("Failed to add plan. Status code: " + statusCode);
                            log.error("Response: " + responseBody);
                            return false;
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to execute HTTP request", e);
                }


            } catch (APIManagementException e) {
                String errorMessage = "Failed to get ID from database for : " + apiName;
                //throw MonetizationException as it will be logged and handled by the caller
                throw new MonetizationException(errorMessage, e);
            } catch (KillBillMonetizationException e) {
                String errorMessage = "Failed to create products and plans in stripe for : " + apiName;
                //throw MonetizationException as it will be logged and handled by the caller
                throw new MonetizationException(errorMessage, e);
            } catch (SQLException e) {
                String errorMessage = "Error while retrieving the API ID";
                throw new MonetizationException(errorMessage, e);
            }

    }

    @Override
    public boolean disableMonetization(String s, API api, Map<String, String> map) throws MonetizationException {
        return false;
    }
    /**
     * Get mapping of tiers and billing engine plans
     *
     * @param api API
     * @return tier to billing plan mapping
     * @throws MonetizationException if failed to get tier to billing plan mapping
     */
    public Map<String, String> getMonetizedPoliciesToPlanMapping(API api) throws MonetizationException {
        try (Connection con = APIMgtDBUtil.getConnection()) {
            String apiName = api.getId().getApiName();
            int apiId = ApiMgtDAO.getInstance().getAPIID(api.getUuid(), con);
            //get billing engine product ID for that API
            String billingProductIdForApi = getBillingProductIdForApi(apiId);
            if (StringUtils.isEmpty(billingProductIdForApi)) {
                log.info("No product was found in billing engine for  : " + apiName);
                return new HashMap<String, String>();
            }
            //get tier to billing engine plan mapping
            return KillBillDAO.getTierToBillingEnginePlanMapping(apiId, billingProductIdForApi);
        } catch (KillBillMonetizationException e) {
            String errorMessage = "Failed to get tier to billing engine plan mapping for : " + api.getId().getApiName();
            //throw MonetizationException as it will be logged and handled by the caller
            throw new MonetizationException(errorMessage, e);
        } catch (APIManagementException e) {
            String errorMessage = "Failed to get ID from database for : " + api.getId().getApiName() +
                    " when getting tier to billing engine plan mapping.";
            //throw MonetizationException as it will be logged and handled by the caller
            throw new MonetizationException(errorMessage, e);
        } catch (SQLException e) {
            String errorMessage = "Error while retrieving the API ID";
            throw new MonetizationException(e);
        }
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

    private String getCreatedBy(String  tenantDomain) throws KillBillMonetizationException {


        try {
            JSONObject tenantConfig = APIUtil.getTenantConfig(tenantDomain);
            if (tenantConfig.containsKey(KillBillMonetizationConstants.MONETIZATION_INFO)) {
                JSONObject monetizationInfo = (JSONObject) tenantConfig
                        .get(KillBillMonetizationConstants.MONETIZATION_INFO);
                if (monetizationInfo.containsKey(KillBillMonetizationConstants.BILLING_ENGINE_PLATFORM_CREATED_BY)) {
                    String CreatedByKillBill = monetizationInfo
                            .get(KillBillMonetizationConstants.BILLING_ENGINE_PLATFORM_CREATED_BY).toString();
                    if (StringUtils.isBlank(CreatedByKillBill)) {
                        String errorMessage = "username platform account key is empty for tenant : " +  tenantDomain;
                        throw new KillBillMonetizationException(errorMessage);
                    }
                    return CreatedByKillBill;
                }
            }
        } catch (APIManagementException e) {
            String errorMessage = "Failed to get the configuration for tenant from DB:  " +  tenantDomain;
            log.error(errorMessage);
            throw new KillBillMonetizationException(errorMessage, e);
        }
        return StringUtils.EMPTY;
    }

    private BillingPeriod getPlan(String paln){
        switch (paln) {
                case "week":
                    return BillingPeriod.WEEKLY;
                case "month":
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

    /**
     * Get billing product ID for a given API
     *
     * @param apiId API ID
     * @return billing product ID for the given API
     * @throws KillBillMonetizationException if failed to get billing product ID for the given API
     */
    private String getBillingProductIdForApi(int apiId) throws KillBillMonetizationException {

        String billingProductId = StringUtils.EMPTY;
        billingProductId = KillBillDAO.getBillingEngineProductId(apiId);
        return billingProductId;
    }
    /**
     * Get billing plan ID for a given tier
     *
     * @param apiId    API ID
     * @param tierName tier name
     * @return billing plan ID for a given tier
     * @throws KillBillMonetizationException if failed to get billing plan ID for the given tier
     */
    private String getBillingPlanIdOfTier(int apiId, String tierName) throws KillBillMonetizationException {

        String billingPlanId = StringUtils.EMPTY;
        billingPlanId = KillBillDAO.getBillingEnginePlanIdForTier(apiId, tierName);
        return billingPlanId;
    }

    private String getBillingPlanId(int apiId) throws KillBillMonetizationException {

        String billingplanId = StringUtils.EMPTY;
        billingplanId = KillBillDAO.getBillingEnginePlanId(apiId);
        return billingplanId;
    }
    /**
     * Get billing plan ID for a given tier
     *
     * @param tierUUID tier UUID
     * @return billing plan ID for a given tier
     * @throws KillBillMonetizationException if failed to get billing plan ID for a given tier
     */
    public String getBillingPlanId(String tierUUID) throws KillBillMonetizationException {

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String planId = null;
        try {
            conn = APIMgtDBUtil.getConnection();
            conn.setAutoCommit(false);
            ps = conn.prepareStatement(KillBillMonetizationConstants.GET_BILLING_PLAN_ID);
            ps.setString(1, tierUUID);
            rs = ps.executeQuery();
            while (rs.next()) {
                planId = rs.getString("PLAN_ID");
            }
        } catch (SQLException e) {
            String errorMessage = "Error while getting stripe plan ID for tier UUID : " + tierUUID;
            log.error(errorMessage);
            throw new KillBillMonetizationException(errorMessage, e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, rs);
        }
        return planId;
    }

}
