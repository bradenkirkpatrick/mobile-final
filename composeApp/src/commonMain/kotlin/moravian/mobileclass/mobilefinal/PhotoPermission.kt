package moravian.mobileclass.mobilefinal

import androidx.compose.runtime.Composable

@Composable
expect fun PhotoPermissionHandler(
    content: @Composable (hasPermission: Boolean, requestPermission: () -> Unit) -> Unit
)

