package com.armonia.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.armonia.app.domain.model.Profile
import com.armonia.app.ui.exercises.ExercisesScreen
import com.armonia.app.ui.home.HomeScreen
import com.armonia.app.ui.lessons.LessonDetailScreen
import com.armonia.app.ui.lessons.LessonsScreen
import com.armonia.app.ui.metronome.MetronomeScreen
import com.armonia.app.ui.profile.ProfileScreen
import com.armonia.app.ui.progress.ProgressScreen
import com.armonia.app.ui.scores.ScoreDetailScreen
import com.armonia.app.ui.scores.ScoresScreen
import com.armonia.app.ui.tuner.TunerScreen

object Routes {
    const val HOME = "home"
    const val TUNER = "tuner"
    const val METRONOME = "metronome"
    const val SCORES = "scores"
    const val SCORE_DETAIL = "score/{scoreId}"
    const val LESSONS = "lessons"
    const val LESSON_DETAIL = "lesson/{lessonId}"
    const val EXERCISES = "exercises"
    const val PROGRESS = "progress"
    const val PROFILE = "profile"

    fun score(scoreId: String) = "score/$scoreId"
    fun lesson(lessonId: String) = "lesson/$lessonId"
}

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.HOME, "Home", Icons.Outlined.Home),
    TopLevelDestination(Routes.SCORES, "Spartiti", Icons.Outlined.LibraryMusic),
    TopLevelDestination(Routes.LESSONS, "Lezioni", Icons.Outlined.School),
    TopLevelDestination(Routes.EXERCISES, "Studi", Icons.Outlined.FitnessCenter),
    TopLevelDestination(Routes.PROGRESS, "Progressi", Icons.Outlined.Insights)
)

private val topLevelRoutes = topLevelDestinations.map { it.route }.toSet()

@Composable
fun ArmoniaNavHost(
    profile: Profile,
    onProfileChange: (Profile) -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in topLevelRoutes

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    topLevelDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(Routes.HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label
                                )
                            },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    profile = profile,
                    onNavigate = { route -> navController.navigate(route) }
                )
            }

            composable(Routes.TUNER) {
                TunerScreen(
                    instrument = profile.instrument,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.METRONOME) {
                MetronomeScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.SCORES) {
                ScoresScreen(
                    profile = profile,
                    onOpenScore = { scoreId -> navController.navigate(Routes.score(scoreId)) }
                )
            }

            composable(
                route = Routes.SCORE_DETAIL,
                arguments = listOf(navArgument("scoreId") { type = NavType.StringType })
            ) { entry ->
                ScoreDetailScreen(
                    scoreId = entry.arguments?.getString("scoreId").orEmpty(),
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.LESSONS) {
                LessonsScreen(
                    profile = profile,
                    onOpenLesson = { lessonId -> navController.navigate(Routes.lesson(lessonId)) }
                )
            }

            composable(
                route = Routes.LESSON_DETAIL,
                arguments = listOf(navArgument("lessonId") { type = NavType.StringType })
            ) { entry ->
                LessonDetailScreen(
                    lessonId = entry.arguments?.getString("lessonId").orEmpty(),
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.EXERCISES) {
                ExercisesScreen(profile = profile)
            }

            composable(Routes.PROGRESS) {
                ProgressScreen()
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    profile = profile,
                    onBack = { navController.popBackStack() },
                    onSave = { updated ->
                        onProfileChange(updated)
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
