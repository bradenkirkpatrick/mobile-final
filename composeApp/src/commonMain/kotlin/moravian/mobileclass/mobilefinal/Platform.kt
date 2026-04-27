package moravian.mobileclass.mobilefinal

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
