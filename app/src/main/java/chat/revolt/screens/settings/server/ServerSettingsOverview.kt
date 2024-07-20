package chat.revolt.screens.settings.server

import android.content.Context
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.activities.RevoltTweenFloat
import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.RevoltAPI
import chat.revolt.api.routes.microservices.autumn.uploadToAutumn
import chat.revolt.api.routes.server.patchServer
import chat.revolt.api.schemas.Server
import chat.revolt.components.generic.InlineMediaPicker
import chat.revolt.components.generic.ListHeader
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.http.ContentType
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ServerSettingsOverviewViewModel @Inject constructor(@ApplicationContext val context: Context) :
    ViewModel() {
    var initialServer by mutableStateOf<Server?>(null)

    var serverName by mutableStateOf("")
    var serverDescription by mutableStateOf("")

    var iconModel by mutableStateOf<Any?>(null)
    var iconIsUploading by mutableStateOf(false)
    var iconUploadProgress by mutableFloatStateOf(0f)

    var uploadError by mutableStateOf<String?>(null)
    var updateError by mutableStateOf<String?>(null)

    var bannerModel by mutableStateOf<Any?>(null)
    var bannerIsUploading by mutableStateOf(false)
    var bannerUploadProgress by mutableFloatStateOf(0f)

    fun populateWithServer(serverId: String) {
        val server = RevoltAPI.serverCache[serverId]
        initialServer = server
        server?.let {
            serverName = it.name ?: ""
            serverDescription = it.description ?: ""
            iconModel = it.icon?.let { icon -> "$REVOLT_FILES/icons/${icon.id}" }
            bannerModel = it.banner?.let { banner -> "$REVOLT_FILES/banners/${banner.id}" }
        }
    }

    private fun unsetIcon() {
        iconIsUploading = true
        iconUploadProgress = 0f
        uploadError = null

        initialServer?.id?.let { serverId ->
            viewModelScope.launch {
                try {
                    patchServer(serverId, remove = listOf("Icon"))
                    iconModel = null
                } catch (e: Exception) {
                    updateError = e.message
                }
                iconIsUploading = false
            }
        } ?: run {
            iconIsUploading = false
        }
    }

    fun pickIcon(newModel: Any?) {
        iconModel = newModel
        uploadError = null
        iconUploadProgress = 0f

        val uri = when (newModel) {
            is Uri -> newModel
            is String -> Uri.parse(newModel)
            else -> null
        } ?: run {
            unsetIcon()
            return
        }

        val mFile = File(context.cacheDir, uri.lastPathSegment ?: "icon")

        mFile.outputStream().use { output ->
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.copyTo(output)
            }
        }

        val mime = context.contentResolver.getType(uri)

        if (mime?.endsWith("webp") == true) {
            uploadError = "WebP is not supported"
            return
        }

        viewModelScope.launch {
            iconIsUploading = true
            try {
                val id = uploadToAutumn(
                    mFile,
                    uri.lastPathSegment ?: "icon",
                    "icons",
                    ContentType.parse(mime ?: "image/*"),
                    onProgress = { soFar, outOf ->
                        iconUploadProgress = soFar.toFloat() / outOf.toFloat()
                    }
                )

                patchServer(initialServer?.id ?: "", icon = id)

                iconIsUploading = false
            } catch (e: Exception) {
                uploadError = e.message
                iconUploadProgress = 0f
                iconIsUploading = false
                return@launch
            }
        }
    }

    fun unsetBanner() {
        bannerIsUploading = true
        bannerUploadProgress = 0f
        uploadError = null

        initialServer?.id?.let { serverId ->
            viewModelScope.launch {
                try {
                    patchServer(serverId, remove = listOf("Banner"))
                    bannerModel = null
                } catch (e: Exception) {
                    updateError = e.message
                }
                bannerIsUploading = false
            }
        } ?: run {
            bannerIsUploading = false
        }
    }

    fun pickBanner(newModel: Any?) {
        bannerModel = newModel
        uploadError = null
        bannerUploadProgress = 0f

        val uri = when (newModel) {
            is Uri -> newModel
            is String -> Uri.parse(newModel)
            else -> null
        } ?: run {
            unsetBanner()
            return
        }

        val mFile = File(context.cacheDir, uri.lastPathSegment ?: "banner")

        mFile.outputStream().use { output ->
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.copyTo(output)
            }
        }

        val mime = context.contentResolver.getType(uri)

        if (mime?.endsWith("webp") == true) {
            uploadError = "WebP is not supported"
            return
        }

        viewModelScope.launch {
            bannerIsUploading = true
            try {
                val id = uploadToAutumn(
                    mFile,
                    uri.lastPathSegment ?: "banner",
                    "banners",
                    ContentType.parse(mime ?: "image/*"),
                    onProgress = { soFar, outOf ->
                        bannerUploadProgress = soFar.toFloat() / outOf.toFloat()
                    }
                )

                patchServer(initialServer?.id ?: "", banner = id)

                bannerIsUploading = false
            } catch (e: Exception) {
                uploadError = e.message
                bannerUploadProgress = 0f
                bannerIsUploading = false
                return@launch
            }
        }
    }

    fun updateServer() {
        updateError = null
        viewModelScope.launch {
            try {
                patchServer(
                    initialServer?.id ?: "",
                    name = if (serverName != initialServer?.name) serverName else null,
                    description = if (serverDescription != initialServer?.description) serverDescription else null
                )
                initialServer = initialServer?.copy(
                    name = serverName,
                    description = serverDescription
                )
            } catch (e: Exception) {
                updateError = e.message
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSettingsOverview(
    navController: NavController,
    serverId: String,
    viewModel: ServerSettingsOverviewViewModel = hiltViewModel()
) {
    val currentServer = RevoltAPI.serverCache[serverId]
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(serverId) {
        viewModel.populateWithServer(serverId)
    }

    val serverInfoUpdated by remember(
        currentServer,
        viewModel.serverName,
        viewModel.serverDescription
    ) {
        derivedStateOf {
            currentServer?.let { server ->
                (server.name ?: "") != viewModel.serverName ||
                        (server.description ?: "") != viewModel.serverDescription
            } ?: false
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        text = stringResource(R.string.server_settings_overview),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
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
        floatingActionButton = {
            AnimatedVisibility(
                visible = serverInfoUpdated,
                enter = scaleIn(animationSpec = RevoltTweenFloat),
                exit = scaleOut(animationSpec = RevoltTweenFloat)
            ) {
                FloatingActionButton(onClick = { viewModel.updateServer() }) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.channel_settings_overview_save)
                    )
                }
            }
        }
    ) { pv ->
        Box(
            Modifier
                .padding(pv)
                .imePadding()
        ) {
            currentServer?.let {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {

                    ListHeader {
                        Text(stringResource(R.string.server_settings_overview_info))
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        InlineMediaPicker(
                            currentModel = viewModel.iconModel,
                            onPick = { viewModel.pickIcon(it) },
                            circular = true,
                            mimeType = "image/*",
                            canRemove = true,
                            enabled = !viewModel.iconIsUploading,
                            onRemove = { viewModel.pickIcon(null) },
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                    ) {

                        Spacer(Modifier.height(10.dp))

                        InlineMediaPicker(
                            currentModel = viewModel.bannerModel,
                            onPick = {
                                viewModel.pickBanner(it)
                            },
                            canRemove = true,
                            onRemove = {
                                viewModel.unsetBanner()
                            }
                        )
                    }

                    AnimatedVisibility(visible = viewModel.iconIsUploading || viewModel.bannerIsUploading) {
                        LinearProgressIndicator(
                            progress = { viewModel.iconUploadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 16.dp)
                        )
                    }

                    AnimatedVisibility(visible = viewModel.uploadError != null) {
                        Text(
                            viewModel.uploadError
                                ?: stringResource(R.string.server_settings_overview_update_info_error_fallback),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 16.dp)
                        )
                    }

                    AnimatedVisibility(visible = viewModel.updateError != null) {
                        Text(
                            viewModel.updateError
                                ?: stringResource(R.string.server_settings_overview_update_info_error_fallback),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 16.dp)
                        )
                    }

                    TextField(
                        label = {
                            Text(stringResource(R.string.server_settings_overview_name))
                        },
                        value = viewModel.serverName,
                        onValueChange = { viewModel.serverName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        singleLine = true
                    )

                    TextField(
                        label = {
                            Text(stringResource(R.string.server_settings_overview_description))
                        },
                        placeholder = {
                            Text(stringResource(R.string.server_settings_overview_description_hint))
                        },
                        value = viewModel.serverDescription,
                        onValueChange = { viewModel.serverDescription = it },
                        modifier = Modifier
                            .animateContentSize()
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        singleLine = false,
                        minLines = 3
                    )

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
