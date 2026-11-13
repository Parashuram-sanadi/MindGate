package com.mindgate.presentation.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object Auth : Screen("auth")
    data object Assessment : Screen("assessment")
    data object Result : Screen("result")
    data object AppLimitSetup : Screen("app_limit_setup")
    data object AppPicker : Screen("app_picker")
    data object PermissionSetup : Screen("permission_setup")
    data object CheckIn : Screen("check_in")
    data object AssessmentChoice : Screen("assessment_choice")
    data object Home : Screen("home")
    data object Limits : Screen("limits")
    data object Profile : Screen("profile")
    data object RoleSelection : Screen("role_selection")
    data object ConnectionSettings : Screen("connection_settings")

    // Explore & Recharge
    data object Explore : Screen("explore")
    data object ExplorePlayer : Screen("explore_player")

    // Teacher flow
    data object TeacherAuth : Screen("teacher_auth")
    data object TeacherDashboard : Screen("teacher_dashboard")
    data object TeacherResults : Screen("teacher_results")

    // Parent flow
    data object ParentDashboard : Screen("parent_dashboard")
    data object ChildUsage : Screen("child_usage") // route will be "child_usage/{studentId}"
    data object ParentAlerts : Screen("parent_alerts")
    data object ParentConnection : Screen("parent_connection")
    data object ParentLinkStudent : Screen("parent_link_student")
    data object RoleGate : Screen("role_gate")
}
