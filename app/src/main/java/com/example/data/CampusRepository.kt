package com.example.data

import kotlinx.coroutines.flow.Flow

class CampusRepository(private val database: CampusDatabase) {

    private val orderDao = database.orderDao()
    private val profileDao = database.userProfileDao()
    private val earningDao = database.earningDao()
    private val reportDao = database.reportDao()

    val allOrdersFlow: Flow<List<OrderEntity>> = orderDao.getAllOrdersFlow()
    val allProfilesFlow: Flow<List<UserProfileEntity>> = profileDao.getAllProfilesFlow()
    val allReportsFlow: Flow<List<ReportEntity>> = reportDao.getAllReportsFlow()

    fun getOrdersByCustomerFlow(phone: String): Flow<List<OrderEntity>> {
        return orderDao.getOrdersByCustomerFlow(phone)
    }

    fun getOrdersByPartnerFlow(partnerId: String): Flow<List<OrderEntity>> {
        return orderDao.getOrdersByPartnerFlow(partnerId)
    }

    fun getEarningsFlow(partnerId: String): Flow<List<EarningEntity>> {
        return earningDao.getEarningsByPartnerFlow(partnerId)
    }

    suspend fun getProfileByRegNum(regNum: String): UserProfileEntity? {
        return profileDao.getProfileByRegNum(regNum)
    }

    suspend fun getProfileByPhone(phone: String): UserProfileEntity? {
        return profileDao.getProfileByPhone(phone)
    }

    suspend fun insertProfile(profile: UserProfileEntity) {
        profileDao.insertProfile(profile)
    }

    suspend fun updateProfile(profile: UserProfileEntity) {
        profileDao.updateProfile(profile)
    }

    suspend fun insertOrder(order: OrderEntity): Int {
        return orderDao.insertOrder(order).toInt()
    }

    suspend fun updateOrder(order: OrderEntity) {
        orderDao.updateOrder(order)
    }

    suspend fun updateOrderStatus(orderId: Int, status: String) {
        orderDao.updateOrderStatus(orderId, status)
    }

    suspend fun deleteOrderById(orderId: Int) {
        orderDao.deleteOrderById(orderId)
    }

    suspend fun insertEarning(earning: EarningEntity) {
        earningDao.insertEarning(earning)
    }

    suspend fun insertReport(report: ReportEntity) {
        reportDao.insertReport(report)
    }

    suspend fun updateReport(report: ReportEntity) {
        reportDao.updateReport(report)
    }

    suspend fun updateSuspensionStatus(regNum: String, suspended: Boolean) {
        profileDao.updateSuspensionStatus(regNum, suspended)
    }

    suspend fun updateTrustScore(regNum: String, strikes: Int, score: Int) {
        profileDao.updateTrustScore(regNum, strikes, score)
    }
}
