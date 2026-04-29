package moravian.mobileclass.mobilefinal

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController {
	initKoin()
	App()
}
