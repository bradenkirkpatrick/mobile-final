package moravian.mobileclass.mobilefinal

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

data class MapConfig(
    val defaultLatitude: Double = 35.684,
    val defaultLongitude: Double = -82.548,
    val defaultZoom: Double = 16.0,
)

data class CameraConfig(
    val photoDirectory: String = "MonoPhoto",
)

val coreModule =
    module {
        single<Platform> { getPlatform() }
    }

val featureModule =
    module {
        single { MapConfig() }
        single { CameraConfig() }
    }

private var koinStarted = false

fun initKoin(
    vararg extraModules: Module,
    appDeclaration: KoinApplication.() -> Unit = {},
) {
    if (!koinStarted) {
        startKoin {
            appDeclaration()
            modules(coreModule, featureModule, *extraModules)
        }
        koinStarted = true
    }
}


