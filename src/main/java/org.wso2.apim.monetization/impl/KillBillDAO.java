package org.wso2.apim.monetization.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.policy.SubscriptionPolicy;
import org.wso2.carbon.apimgt.impl.dao.ApiMgtDAO;
import org.wso2.carbon.apimgt.impl.utils.APIMgtDBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class KillBillDAO {
    private static final Log log = LogFactory.getLog(KillBillDAO.class);
    private static ApiMgtDAO apiMgtDAO = ApiMgtDAO.getInstance();

    public static void addMonetizationPlanData(SubscriptionPolicy policy, String productId, String planId)
            throws KillBillMonetizationException {

        Connection conn = null;
        PreparedStatement policyStatement = null;
        try {
            conn = APIMgtDBUtil.getConnection();
            conn.setAutoCommit(false);
            policyStatement = conn.prepareStatement(KillBillMonetizationConstants.INSERT_MONETIZATION_PLAN_DATA_SQL);
            policyStatement.setString(1, apiMgtDAO.getSubscriptionPolicy(policy.getPolicyName(),
                    policy.getTenantId()).getUUID());
            policyStatement.setString(2, productId);
            policyStatement.setString(3, planId);
            policyStatement.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    String errorMessage = "Failed to rollback adding monetization plan for : " + policy.getPolicyName();
                    log.error(errorMessage);
                    throw new KillBillMonetizationException(errorMessage, ex);
                }
            }
            String errorMessage = "Failed to add monetization plan for : " + policy.getPolicyName();
            log.error(errorMessage);
            throw new KillBillMonetizationException(errorMessage, e);
        } catch (APIManagementException e) {
            String errorMessage = "Failed to get subscription policy : " + policy.getPolicyName() +
                    " from database when creating stripe plan.";
            log.error(errorMessage);
            throw new KillBillMonetizationException(errorMessage, e);
        } finally {
            APIMgtDBUtil.closeAllConnections(policyStatement, conn, null);
        }
    }

    public static void addMonetizationData(int apiId, String productId, Map<String, String> tierPlanMap)
            throws KillBillMonetizationException {

        PreparedStatement preparedStatement = null;
        Connection connection = null;
        boolean initialAutoCommit = false;
        try {
            if (!tierPlanMap.isEmpty()) {
                connection = APIMgtDBUtil.getConnection();
                preparedStatement = connection.prepareStatement(KillBillMonetizationConstants.ADD_MONETIZATION_DATA_SQL);
                initialAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                for (Map.Entry<String, String> entry : tierPlanMap.entrySet()) {
                    preparedStatement.setInt(1, apiId);
                    preparedStatement.setString(2, entry.getKey());
                    preparedStatement.setString(3, productId);
                    preparedStatement.setString(4, entry.getValue());
                    preparedStatement.addBatch();
                }
                preparedStatement.executeBatch();
                connection.commit();
            }
        } catch (SQLException e) {
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException ex) {
                String errorMessage = "Failed to rollback add monetization data for API : " + apiId;
                log.error(errorMessage, e);
                throw new KillBillMonetizationException(errorMessage, e);
            } finally {
                APIMgtDBUtil.setAutoCommit(connection, initialAutoCommit);
            }
        } finally {
            APIMgtDBUtil.closeAllConnections(preparedStatement, connection, null);
        }
    }

    /**
     * This method is used to get the product id in the billing engine for a give API
     *
     * @param apiId API ID
     * @return billing engine product ID of the give API
     */
    public static String getBillingEngineProductId(int apiId) throws KillBillMonetizationException {

        String billingEngineProductId = null;
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = APIMgtDBUtil.getConnection();
            connection.setAutoCommit(false);
            statement = connection.prepareStatement(KillBillMonetizationConstants.GET_BILLING_ENGINE_PRODUCT_BY_API);
            statement.setInt(1, apiId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                billingEngineProductId = rs.getString("STRIPE_PRODUCT_ID");
            }
            connection.commit();
        } catch (SQLException e) {
            String errorMessage = "Failed to get billing engine product ID of API : " + apiId;
            log.error(errorMessage);
            throw new KillBillMonetizationException(errorMessage, e);
        } finally {
            APIMgtDBUtil.closeAllConnections(statement, connection, null);
        }
        return billingEngineProductId;


    }

    public static String getBillingEnginePlanIdForTier(int apiID, String tierName) throws KillBillMonetizationException {

        Connection connection = null;
        PreparedStatement statement = null;
        String billingEnginePlanId = StringUtils.EMPTY;
        try {
            connection = APIMgtDBUtil.getConnection();
            connection.setAutoCommit(false);
            statement = connection.prepareStatement(KillBillMonetizationConstants.GET_BILLING_PLAN_FOR_TIER);
            statement.setInt(1, apiID);
            statement.setString(2, tierName);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                billingEnginePlanId = rs.getString("STRIPE_PLAN_ID");
            }
            connection.commit();
        } catch (SQLException e) {
            String errorMessage = "Failed to get billing plan ID tier : " + tierName;
            log.error(errorMessage, e);
            throw new KillBillMonetizationException(errorMessage, e);
        } finally {
            APIMgtDBUtil.closeAllConnections(statement, connection, null);
        }
        return billingEnginePlanId;
    }
    public static String getBillingEnginePlanId(int apiID) throws KillBillMonetizationException {

        Connection connection = null;
        PreparedStatement statement = null;
        String billingEnginePlanId = StringUtils.EMPTY;
        try {
            connection = APIMgtDBUtil.getConnection();
            connection.setAutoCommit(false);
            statement = connection.prepareStatement(KillBillMonetizationConstants.GET_BILLING_PLAN_NAME);
            statement.setInt(1, apiID);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                billingEnginePlanId = rs.getString("TIER_ID");
            }
            connection.commit();
        } catch (SQLException e) {
            String errorMessage = "Failed to get billing API ID tier : " + apiID;
            log.error(errorMessage, e);
            throw new KillBillMonetizationException(errorMessage, e);
        } finally {
            APIMgtDBUtil.closeAllConnections(statement, connection, null);
        }
        return billingEnginePlanId;
    }

    /**
     * Get billing plan ID for a given tier
     *
     * @param tierUUID tier UUID
     * @return billing plan ID for a given tier
     * @throws KillBillMonetizationException if failed to get billing plan ID for a given tier
     */
    public static String getBillingPlanId(String tierUUID) throws KillBillMonetizationException {

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


    /**
     * This method is used to get stripe plan and tier mapping
     *
     * @param apiID           API ID
     * @param stripeProductId stripe product ID
     * @return mapping between tier and stripe plans
     * @throws KillBillMonetizationException if failed to get mapping between tier and stripe plans
     */
    public static Map<String, String> getTierToBillingEnginePlanMapping(int apiID, String stripeProductId)
            throws KillBillMonetizationException {

        Map<String, String> stripePlanTierMap = new HashMap<String, String>();
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = APIMgtDBUtil.getConnection();
            connection.setAutoCommit(false);
            statement = connection.prepareStatement(KillBillMonetizationConstants.GET_BILLING_PLANS_BY_PRODUCT);
            statement.setInt(1, apiID);
            statement.setString(2, stripeProductId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                String tierName = rs.getString("TIER_NAME");
                String stripePlanId = rs.getString("STRIPE_PLAN_ID");
                stripePlanTierMap.put(tierName, stripePlanId);
            }
            connection.commit();
        } catch (SQLException e) {
            String errorMessage = "Failed to get stripe plan and tier mapping for API : " + apiID;
            log.error(errorMessage);
            throw new KillBillMonetizationException(errorMessage, e);
        } finally {
            APIMgtDBUtil.closeAllConnections(statement, connection, null);
        }
        return stripePlanTierMap;
    }
}
