package org.wso2.apim.monetization.impl;

public class KillBillMonetizationConstants {
    public static final String MONETIZATION_INFO = "MonetizationInfo";
    public static final String BILLING_ENGINE_PLATFORM_USERNAME = "BillingEnginePlatformUserName";//admin
    public static final String BILLING_ENGINE_PLATFORM_PASSWORD = "BillingEnginePlatformPassword";//password
    public static final String BILLING_ENGINE_PLATFORM_API_KEY = "BillingEnginePlatformApiKey";//inetum
    public static final String BILLING_ENGINE_PLATFORM_API_SECRET = "BillingEnginePlatformApiSecret";//inetum

    public static final String BILLING_ENGINE_PLATFORM_CREATED_BY = "BillingEnginePlatformCreatedBy";//charouni
    public static final String ADD_MONETIZATION_DATA_SQL = "INSERT INTO AM_MONETIZATION VALUES (?,?,?,?)";
    public static final String DELETE_MONETIZATION_DATA_SQL = "DELETE FROM AM_MONETIZATION WHERE API_ID = ?";
    public static final String GET_BILLING_ENGINE_PRODUCT_BY_API = "SELECT STRIPE_PRODUCT_ID FROM AM_MONETIZATION WHERE API_ID = ? ";
    public static final String GET_BILLING_ENGINE_SUBSCRIPTION_ID = "SELECT SUBSCRIPTION_ID FROM " +
            "AM_MONETIZATION_SUBSCRIPTIONS " +
            "WHERE SUBSCRIBED_APPLICATION_ID = ? AND SUBSCRIBED_API_ID = ?";
    public static final String GET_BILLING_PLANS_BY_PRODUCT = "SELECT TIER_NAME, STRIPE_PLAN_ID FROM AM_MONETIZATION " +
            "WHERE API_ID = ? AND STRIPE_PRODUCT_ID = ?";
    public static final String GET_BILLING_PLAN_FOR_TIER = "SELECT STRIPE_PLAN_ID FROM AM_MONETIZATION " +
            "WHERE API_ID = ? AND TIER_NAME = ?";
    public static final String INSERT_MONETIZATION_PLAN_DATA_SQL =
            "INSERT INTO AM_POLICY_PLAN_MAPPING (POLICY_UUID, PRODUCT_ID, PLAN_ID) VALUES (?,?,?)";
    public static final String UPDATE_MONETIZATION_PLAN_ID_SQL = "UPDATE AM_POLICY_PLAN_MAPPING SET PLAN_ID = ? " +
            "WHERE POLICY_UUID = ? AND PRODUCT_ID = ?";
    public static final String DELETE_MONETIZATION_PLAN_DATA = "DELETE FROM AM_POLICY_PLAN_MAPPING WHERE " +
            "POLICY_UUID = ?";
    public static final String GET_BILLING_PLAN_DATA = "SELECT PRODUCT_ID, PLAN_ID FROM AM_POLICY_PLAN_MAPPING " +
            "WHERE POLICY_UUID = ?";
    public static final String GET_BILLING_PLAN_ID = "SELECT PLAN_ID FROM AM_POLICY_PLAN_MAPPING " +
            "WHERE POLICY_UUID = ?";
    public static final String GET_SUBSCRIPTION_UUID = "SELECT UUID FROM AM_SUBSCRIPTION WHERE SUBSCRIPTION_ID = ?";

    public static final String ADD_BE_PLATFORM_CUSTOMER_SQL =
            " INSERT" +
                    " INTO AM_MONETIZATION_PLATFORM_CUSTOMERS (SUBSCRIBER_ID, TENANT_ID, CUSTOMER_ID)" +
                    " VALUES (?,?,?)";

    public static final String ADD_BE_SHARED_CUSTOMER_SQL =
            " INSERT" +
                    " INTO AM_MONETIZATION_SHARED_CUSTOMERS (APPLICATION_ID,  API_PROVIDER," +
                    " TENANT_ID, SHARED_CUSTOMER_ID, PARENT_CUSTOMER_ID)" +
                    " VALUES (?,?,?,?,?)";

    public static final String ADD_BE_SUBSCRIPTION_SQL =
            " INSERT" +
                    " INTO AM_MONETIZATION_SUBSCRIPTIONS (SUBSCRIBED_API_ID, SUBSCRIBED_APPLICATION_ID," +
                    " TENANT_ID, SHARED_CUSTOMER_ID, SUBSCRIPTION_ID)" +
                    " VALUES ((SELECT API_ID FROM AM_API WHERE API_UUID = ?),?,?,?,?)";

    public static final String GET_BE_PLATFORM_CUSTOMER_SQL =
            "SELECT" +
                    " ID, CUSTOMER_ID" +
                    " FROM AM_MONETIZATION_PLATFORM_CUSTOMERS" +
                    " WHERE" +
                    " SUBSCRIBER_ID=? AND TENANT_ID=?";

    public static final String GET_BE_SHARED_CUSTOMER_SQL =
            " SELECT" +
                    " ID, SHARED_CUSTOMER_ID" +
                    " FROM AM_MONETIZATION_SHARED_CUSTOMERS" +
                    " WHERE" +
                    " APPLICATION_ID=? AND API_PROVIDER=? AND TENANT_ID=?";

    public static final String GET_BE_SUBSCRIPTION_SQL =
            " SELECT" +
                    " ID, SUBSCRIPTION_ID" +
                    " FROM AM_MONETIZATION_SUBSCRIPTIONS" +
                    " WHERE" +
                    " SUBSCRIBED_APPLICATION_ID=? " +
                    " AND SUBSCRIBED_API_ID=(SELECT API_ID FROM AM_API WHERE API_UUID=?)" +
                    " AND TENANT_ID=?";

    public static final String DELETE_BE_SUBSCRIPTION_SQL = "DELETE FROM AM_MONETIZATION_SUBSCRIPTIONS WHERE ID=?";


}
