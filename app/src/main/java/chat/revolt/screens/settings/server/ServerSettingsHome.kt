package chat.revolt.screens.settings.server

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.PermissionBit
import chat.revolt.api.internals.hasPermission
import chat.revolt.internals.extensions.rememberServerPermissions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSettingsHome(navController: NavController, serverId: String) {
    val server = RevoltAPI.serverCache[serverId]
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val permissions by rememberServerPermissions(serverId)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    server?.name?.let {
                        Text(
                            text = it,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
            )
        },
    ) { pv ->
        Box(Modifier.padding(pv)) {
            server?.let {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    if (permissions.hasPermission(PermissionBit.ManageServer)) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = stringResource(id = R.string.server_settings_overview)
                                )
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                )
                            },
                            modifier = Modifier
                                .testTag("server_settings_view_overview")
                                .clickable {
                                    navController.navigate("settings/server/${server.id}/overview")
                                }
                        )
                    }

                }
            } ?: run {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}