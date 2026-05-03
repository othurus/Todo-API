package com.example.rssreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.rssreader.ui.screens.ArticleDetailScreen
import com.example.rssreader.ui.screens.HomeScreen
import com.example.rssreader.ui.theme.RssReaderTheme
import com.example.rssreader.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as RssReaderApp

        setContent {
            RssReaderTheme {
                val viewModel: MainViewModel = viewModel(
                    factory = MainViewModel.Factory(app.repository)
                )
                val uiState by viewModel.uiState.collectAsState()
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onArticleClick = { article ->
                                viewModel.selectArticle(article)
                                navController.navigate("article/${article.id}")
                            }
                        )
                    }
                    composable(
                        route = "article/{articleId}",
                        arguments = listOf(navArgument("articleId") { type = NavType.LongType })
                    ) {
                        val article = uiState.selectedArticle
                        if (article != null) {
                            ArticleDetailScreen(
                                article = article,
                                onBack = {
                                    navController.popBackStack()
                                    viewModel.selectArticle(null)
                                },
                                onToggleBookmark = { viewModel.toggleBookmark(article) }
                            )
                        }
                    }
                }
            }
        }
    }
}
