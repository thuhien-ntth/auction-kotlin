package com.auction.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.auction.app.ui.screens.exhibition.ExhibitionManagementScreen
import com.auction.app.ui.screens.login.LoginScreen
import com.auction.app.ui.screens.register.RegisterScreen
import com.auction.app.ui.screens.verify.VerifyEmailScreen
import com.auction.app.ui.screens.participating.ParticipatingScreen
import com.auction.app.ui.screens.detail.ProductDetailScreen
import com.auction.app.ui.screens.register_product.ProductRegisterScreen
import com.auction.app.ui.screens.search.ProductSearchScreen
import com.auction.app.ui.screens.won.WonScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.REGISTER) {
            RegisterScreen(navController = navController)
        }

        composable(
            route = Routes.VERIFY_EMAIL,
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email").orEmpty()
            VerifyEmailScreen(navController = navController, email = email)
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                navController = navController,
                onLoginSuccess = {
                    navController.navigate(Routes.PRODUCT_SEARCH) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.PRODUCT_SEARCH) {
            ProductSearchScreen(
                navController = navController,
                onOpenProduct = { productId ->
                    navController.navigate(Routes.productDetail(productId))
                }
            )
        }

        composable(
            route = Routes.PRODUCT_DETAIL,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId").orEmpty()
            ProductDetailScreen(productId = productId, navController = navController)
        }

        composable(Routes.EXHIBITION_MANAGEMENT) {
            ExhibitionManagementScreen(
                navController = navController,
                onOpenProduct = { productId ->
                    navController.navigate(Routes.productDetail(productId))
                }
            )
        }

                composable(Routes.PRODUCT_EDIT) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            com.auction.app.ui.screens.register_product.ProductEditScreen(productId, navController)
        }
        composable(Routes.PRODUCT_REGISTER) {
            ProductRegisterScreen(navController = navController)
        }

        composable(Routes.PARTICIPATING) {
            ParticipatingScreen(
                navController = navController,
                onOpenProduct = { productId ->
                    navController.navigate(Routes.productDetail(productId))
                }
            )
        }

        composable(Routes.WON) {
            WonScreen(
                navController = navController,
                onOpenProduct = { productId ->
                    navController.navigate(Routes.productDetail(productId))
                }
            )
        }

        composable(Routes.ADMIN_DASHBOARD) {
            com.auction.app.ui.screens.admin.AdminDashboardScreen(
                navController = navController,
                onOpenProduct = { productId ->
                    navController.navigate(Routes.productDetail(productId))
                }
            )
        }
    }
}
