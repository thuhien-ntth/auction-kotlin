package com.auction.app.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val VERIFY_EMAIL = "verify_email/{email}"
    const val PRODUCT_SEARCH = "product_search"
    const val PRODUCT_DETAIL = "product_detail/{productId}"
    const val EXHIBITION_MANAGEMENT = "exhibition_management"
    const val PRODUCT_REGISTER = "product_register"
    const val PARTICIPATING = "participating"
    const val WON = "won"
    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val PRODUCT_EDIT = "product_edit/{productId}"

    fun verifyEmail(email: String) = "verify_email/$email"
    fun productDetail(productId: String) = "product_detail/$productId"
    fun productEdit(productId: String) = "product_edit/$productId"
}
