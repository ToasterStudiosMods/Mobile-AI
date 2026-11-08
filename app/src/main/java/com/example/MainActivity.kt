package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ClusterScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ModelsScreen
import com.example.ui.screens.RunScreen
import com.example.ui.screens.ServerScreen
import com.example.ui.theme.EdgeloomBackground
import com.example.ui.theme.EdgeloomBorder
import com.example.ui.theme.EdgeloomCard
import com.example.ui.theme.EdgeloomOnlineGreen
import com.example.ui.theme.EdgeloomPrimaryLime
import com.example.ui.theme.EdgeloomSurface
import com.example.ui.theme.EdgeloomTextMuted
import com.example.ui.theme.EdgeloomTextPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = viewModel()
                EdgeloomApp(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EdgeloomApp(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val serverConfig by viewModel.serverConfig.collectAsState()

    // Handle back button: return to Home if on another tab
    if (currentScreen != AppScreen.HOME) {
        BackHandler {
            viewModel.navigateTo(AppScreen.HOME)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = EdgeloomBackground,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EdgeloomBackground)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(end = 4.dp)
                        ) {
                            // Logo and Title
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { viewModel.navigateTo(AppScreen.HOME) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(EdgeloomPrimaryLime),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = "Edgeloom Logo",
                                        tint = EdgeloomBackground,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Edgeloom",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp,
                                    color = EdgeloomTextPrimary
                                )
                            }

                            // Server Status Pill (links to Server tab)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (serverConfig.isRunning) EdgeloomPrimaryLime.copy(alpha = 0.12f) else EdgeloomCard)
                                    .border(
                                        1.dp,
                                        if (serverConfig.isRunning) EdgeloomPrimaryLime.copy(alpha = 0.4f) else EdgeloomBorder,
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable { viewModel.navigateTo(AppScreen.SERVER) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(
                                                if (serverConfig.isRunning) EdgeloomPrimaryLime else EdgeloomTextMuted,
                                                CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (serverConfig.isRunning) ":${serverConfig.port} live" else "API off",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (serverConfig.isRunning) EdgeloomPrimaryLime else EdgeloomTextMuted
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = EdgeloomBackground
                    )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(EdgeloomBorder)
                )
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                color = EdgeloomSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, EdgeloomBorder)
            ) {
                NavigationBar(
                    containerColor = EdgeloomSurface,
                    contentColor = EdgeloomTextPrimary,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    val navItems = listOf(
                        Triple(AppScreen.HOME, "Home", Icons.Default.Home),
                        Triple(AppScreen.MODELS, "Models", Icons.Default.Dns),
                        Triple(AppScreen.RUN, "Run", Icons.Default.PlayArrow),
                        Triple(AppScreen.CLUSTER, "Cluster", Icons.Default.Hub),
                        Triple(AppScreen.SERVER, "Server", Icons.Default.Terminal)
                    )

                    navItems.forEach { (screen, label, icon) ->
                        val selected = currentScreen == screen
                        NavigationBarItem(
                            selected = selected,
                            onClick = { viewModel.navigateTo(screen) },
                            icon = { Icon(icon, contentDescription = label) },
                            label = {
                                Text(
                                    label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = EdgeloomPrimaryLime,
                                selectedTextColor = EdgeloomPrimaryLime,
                                unselectedIconColor = EdgeloomTextMuted,
                                unselectedTextColor = EdgeloomTextMuted,
                                indicatorColor = EdgeloomPrimaryLime.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    AppScreen.HOME -> HomeScreen(viewModel = viewModel)
                    AppScreen.MODELS -> ModelsScreen(viewModel = viewModel)
                    AppScreen.RUN -> RunScreen(viewModel = viewModel)
                    AppScreen.CLUSTER -> ClusterScreen(viewModel = viewModel)
                    AppScreen.SERVER -> ServerScreen(viewModel = viewModel)
                }
            }
        }
    }
}
