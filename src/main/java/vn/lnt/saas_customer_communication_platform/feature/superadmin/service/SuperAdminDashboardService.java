package vn.lnt.saas_customer_communication_platform.feature.superadmin.service;

import vn.lnt.saas_customer_communication_platform.feature.superadmin.dto.SuperAdminDashboardResponse;

public interface SuperAdminDashboardService {

    SuperAdminDashboardResponse getDashboardStats(String currentUserEmail);
}

