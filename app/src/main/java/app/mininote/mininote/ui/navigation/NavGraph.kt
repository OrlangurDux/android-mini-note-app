package app.mininote.mininote.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.mininote.mininote.ui.auth.ForgotCodeScreen
import app.mininote.mininote.ui.auth.ForgotDoneScreen
import app.mininote.mininote.ui.auth.ForgotEmailScreen
import app.mininote.mininote.ui.auth.ForgotNewPasswordScreen
import app.mininote.mininote.ui.auth.ForgotPasswordViewModel
import app.mininote.mininote.ui.auth.LoginOtpScreen
import app.mininote.mininote.ui.auth.LoginScreen
import app.mininote.mininote.ui.auth.SignupScreen
import app.mininote.mininote.ui.auth.SignupSentScreen
import app.mininote.mininote.ui.SessionExpiredScreen
import app.mininote.mininote.ui.about.AboutScreen
import app.mininote.mininote.ui.home.HomeScreen
import app.mininote.mininote.ui.notes.NoteDetailScreen
import app.mininote.mininote.ui.servers.ServersScreen

@Composable
fun MiniNoteNavHost(startDestination: Route) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDestination) {
        composable<Route.Login> {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(Route.Home) {
                        popUpTo<Route.Login> { inclusive = true }
                    }
                },
                onNavigateToSignup = { navController.navigate(Route.Signup) },
                onNavigateToForgotPassword = { navController.navigate(Route.ForgotGraph) },
                onNavigateToServers = { navController.navigate(Route.Servers) },
                onMfaRequired = { mfaToken, expiresIn ->
                    navController.navigate(Route.LoginOtp(mfaToken = mfaToken, expiresInSeconds = expiresIn))
                },
            )
        }
        composable<Route.LoginOtp> { entry ->
            val route: Route.LoginOtp = entry.toRoute()
            LoginOtpScreen(
                mfaToken = route.mfaToken,
                expiresInSeconds = route.expiresInSeconds,
                onLoggedIn = {
                    navController.navigate(Route.Home) {
                        popUpTo<Route.Login> { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable<Route.Signup> {
            SignupScreen(
                onSignedUp = { email ->
                    navController.navigate(Route.SignupSent(email)) {
                        popUpTo<Route.Signup> { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable<Route.SignupSent> { entry ->
            val route: Route.SignupSent = entry.toRoute()
            SignupSentScreen(
                email = route.email,
                onContinue = {
                    navController.navigate(Route.Home) {
                        popUpTo<Route.Login> { inclusive = true }
                    }
                },
            )
        }
        forgotPasswordGraph(navController)
        composable<Route.Servers> {
            ServersScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.Home> {
            HomeScreen(
                onOpenNoteDetail = { localId, startInEdit ->
                    navController.navigate(Route.NoteDetail(localId = localId, startInEdit = startInEdit))
                },
                onNavigateToAbout = { navController.navigate(Route.About) },
                onLoggedOut = {
                    navController.navigate(Route.Login) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onSessionExpired = {
                    navController.navigate(Route.SessionExpired) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable<Route.NoteDetail> { entry ->
            val route: Route.NoteDetail = entry.toRoute()
            NoteDetailScreen(
                localId = route.localId,
                startInEdit = route.startInEdit,
                onBack = { navController.popBackStack() },
            )
        }
        composable<Route.About> {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.SessionExpired> {
            SessionExpiredScreen(
                onNavigateToLogin = {
                    navController.navigate(Route.Login) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}

private fun NavGraphBuilder.forgotPasswordGraph(navController: NavHostController) {
    navigation<Route.ForgotGraph>(startDestination = Route.ForgotEmail) {
        composable<Route.ForgotEmail> { entry ->
            val parentEntry = remember(entry) { navController.getBackStackEntry(Route.ForgotGraph) }
            val viewModel: ForgotPasswordViewModel = hiltViewModel(parentEntry)
            ForgotEmailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToCode = { navController.navigate(Route.ForgotCode) },
            )
        }
        composable<Route.ForgotCode> { entry ->
            val parentEntry = remember(entry) { navController.getBackStackEntry(Route.ForgotGraph) }
            val viewModel: ForgotPasswordViewModel = hiltViewModel(parentEntry)
            ForgotCodeScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToNew = { navController.navigate(Route.ForgotNew) },
            )
        }
        composable<Route.ForgotNew> { entry ->
            val parentEntry = remember(entry) { navController.getBackStackEntry(Route.ForgotGraph) }
            val viewModel: ForgotPasswordViewModel = hiltViewModel(parentEntry)
            ForgotNewPasswordScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToDone = { navController.navigate(Route.ForgotDone) },
            )
        }
        composable<Route.ForgotDone> {
            ForgotDoneScreen(
                onNavigateToLogin = {
                    navController.navigate(Route.Login) {
                        popUpTo<Route.ForgotGraph> { inclusive = true }
                    }
                },
            )
        }
    }
}
