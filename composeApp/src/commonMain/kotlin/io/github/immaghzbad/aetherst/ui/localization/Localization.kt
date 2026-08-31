package io.github.immaghzbad.aetherst.shared.ui.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(val code: String, val displayName: String) {
    SYSTEM("system", "System Default / پیش‌فرض"),
    ENGLISH("en", "English"),
    PERSIAN("fa", "فارسی (Persian)")
}

data class Strings(
    val appName: String = "Aether Ng",
    val appSubtitle: String = "Advanced Secure Tunneling Client",
    
    // Navigation
    val tabDashboard: String = "Dashboard",
    val tabSettings: String = "Settings",
    val tabLogs: String = "Logs",
    val tabAbout: String = "About",
    
    // Connection Status
    val statusDisconnected: String = "Disconnected",
    val statusConnecting: String = "Connecting...",
    val statusValidating: String = "Validating...",
    val statusConnected: String = "Connected",
    val statusReconnecting: String = "Reconnecting...",
    val statusStopping: String = "Stopping...",
    val statusError: String = "Error",
    
    // Actions
    val actionConnect: String = "Connect",
    val actionDisconnect: String = "Disconnect",
    val actionCancel: String = "Cancel",
    val actionReset: String = "Reset",
    val actionSave: String = "Save",
    val actionApply: String = "Apply",
    val actionCopy: String = "Copy",
    val actionShare: String = "Share",
    val actionDelete: String = "Delete",
    val actionSearch: String = "Search",
    val actionRestart: String = "Restart",
    val actionOptimize: String = "Optimize",
    
    // Dashboard telemetry
    val labelUplink: String = "Upload",
    val labelDownlink: String = "Download",
    val labelSpeed: String = "Speed",
    val labelPing: String = "Ping",
    val labelLatency: String = "Latency",
    val labelTraffic: String = "Traffic",
    val labelGateway: String = "Gateway",
    val labelServer: String = "Server",
    val labelIpAddress: String = "IP Address",
    val labelDuration: String = "Duration",
    
    // Settings Categories
    val settingsTitle: String = "Aether Ng Settings",
    val settingsSubtitle: String = "Configure engine protocols, obfuscation & transport",
    val catSpeedTest: String = "Internet Speed Test",
    val catSpeedTestSub: String = "Measure download, upload, ping & jitter",
    val catAutoDetect: String = "Smart Auto-Detect",
    val catAutoDetectSub: String = "Detect the best protocol & settings for your network",
    val catPresets: String = "Configuration Profiles",
    val catPresetsSub: String = "Presets & manual tweaks",
    val catConnection: String = "Connection & Tunneling",
    val catConnectionSub: String = "Mode, engine, split tunneling, routing",
    val catProtocol: String = "Protocol & Transport",
    val catProtocolSub: String = "MASQUE, H2, ECH, obfuscation, MTU",
    val catZeroTrust: String = "Cloudflare Zero Trust",
    val catZeroTrustSub: String = "Team, gateway & authentication",
    val catNetwork: String = "Network Parameters",
    val catNetworkSub: String = "SOCKS5, HTTP, ports, DNS, peer",
    val catSecurity: String = "Security & Reliability",
    val catSecuritySub: String = "Kill switch, IPv6 leak, reconnect",
    val catDiagnostics: String = "Diagnostics & Core",
    val catDiagnosticsSub: String = "Logging, perf, upstream proxy",
    val catHevEngine: String = "HEV Engine",
    val catHevEngineSub: String = "Log level, timeouts, session limits (Advanced)",
    val catSystem: String = "System & Maintenance",
    val catSystemSub: String = "App theme, backup, restore, reset",
    
    // Themes
    val themeTitle: String = "App Theme",
    val themeSystem: String = "System Default",
    val themeDark: String = "Dark Mode",
    val themeLight: String = "Light Mode",
    
    // Backup & Reset
    val backupExportTitle: String = "Full Configuration Backup",
    val backupExportSub: String = "Export all settings to .astf file",
    val backupImportTitle: String = "Restore from Backup",
    val backupImportSub: String = "Import settings from an .astf file",
    val resetTitle: String = "Reset to Factory Defaults",
    val resetSub: String = "Wipe all custom tweaks and restart",
    
    // Dashboard Extra Strings
    val tunnelTitle: String = "Aether Ng Tunnel",
    val tunnelSubtitleTunnel: String = "Secure & Private Tunneling",
    val tunnelSubtitleProxy: String = "High-Performance Local Proxy",
    val statusProtectedConnected: String = "PROTECTED & CONNECTED",
    val statusProxyActive: String = "PROXY ACTIVE",
    val statusFindingServers: String = "FINDING SERVERS...",
    val statusEstablishingLink: String = "ESTABLISHING LINK...",
    val statusReconnectingUpper: String = "RECONNECTING...",
    val statusStoppingUpper: String = "STOPPING...",
    val statusErrorUpper: String = "CONNECTION ERROR",
    val statusReadyToConnect: String = "READY TO CONNECT",
    val swipeToConnect: String = "SWIPE TO CONNECT",
    val swipeToDisconnect: String = "SWIPE TO DISCONNECT",
    val releaseToDisconnect: String = "RELEASE TO DISCONNECT",
    val startingTunnel: String = "STARTING TUNNEL...",
    val swipeToRetry: String = "SWIPE TO RETRY",
    val labelUploadUpper: String = "UPLOAD",
    val labelDownloadUpper: String = "DOWNLOAD",
    val labelBypass: String = "BYPASS",
    val labelSpeedUpper: String = "SPEED",
    val labelNetworkUpper: String = "NETWORK",
    val labelShowPublicIp: String = "SHOW PUBLIC IP",
    val labelLocatingIp: String = "LOCATING YOUR IP...",
    val labelCouldNotFindIp: String = "COULD NOT FIND IP",
    val labelNoUplink: String = "NO UPLINK",
    val labelLanguageTitle: String = "Language / زبان",
    val labelSupportTitle: String = "Support Aether Ng",
    val labelSupportMessage: String = "Aether Ng is a free and open-source project developed in our spare time.\nIf you find it useful, please consider joining our official Telegram channel.\nYou will get instant updates about new releases, new features, bug fixes and important announcements.\nYour support keeps the project alive and growing!",
    val labelSupportJoin: String = "Join Telegram Channel",
    val labelSupportDismiss: String = "Dismiss",
    val connFailedMsg: String = "Connection failed. Please try reconnecting.",

    // Live Ping & Signal Visualizer
    val livePingTitle: String = "LIVE PING & SIGNAL",
    val liveSignalQuality: String = "SIGNAL",
    val labelLatencyUpper: String = "LATENCY",
    val labelJitter: String = "JITTER",
    val latencyOptimal: String = "EXCELLENT",
    val latencyGood: String = "GOOD",
    val latencyHigh: String = "HIGH",
    val latencyIdle: String = "STANDBY",
    val tapToTestPing: String = "TAP TO TEST",

    // Speed Chart & Live Traffic
    val speedChartTitle: String = "LIVE TRAFFIC MONITOR",
    val labelLiveSpeed: String = "SPEED",
    val labelTotalSent: String = "SENT",
    val labelTotalRecv: String = "RECEIVED",
    val settingsShowLivePing: String = "Show Live Ping & Signal Widget",
    val settingsShowLivePingSub: String = "Display real-time latency & wave visualizer on Dashboard",
    val settingsShowTrafficChart: String = "Show Live Speed & Traffic Chart",
    val settingsShowTrafficChartSub: String = "Display real-time bandwidth wave graph on Dashboard",

    // About
    val aboutOverviewTitle: String = "Project Overview",
    val aboutCoreTitle: String = "The Aether Core",
    val aboutHevTitle: String = "Native HEV Stack",
    val aboutBridgeTitle: String = "SocksTunBridge (Kotlin)",
    val aboutDevLinksTitle: String = "Dev Links & Source",
    val aboutTelegramTitle: String = "Telegram Channel",
    val aboutTelegramSub: String = "Telegram channel for support, chat, and dev updates.",
    val aboutInstagramTitle: String = "Instagram Contact",
    val aboutInstagramSub: String = "Follow Aliem061 on Instagram for updates.",
    val aboutAetherRepoTitle: String = "Aether Repository",
    val aboutAetherRepoSub: String = "The engine's source code and protocol implementation.",
    val aboutHevRepoTitle: String = "HEV Stack Source",
    val aboutHevRepoSub: String = "Native C implementation of the TUN-to-SOCKS bridge.",
    val aboutFooterBuiltWith: String = "Built with ",
    val aboutFooterTeam: String = " by PowerSigma Team | ادیت شده توسط aliem",
    val aboutFooterDisclaimer: String = "Aether Ng is an independent client project. Aether core is developed by CluvexStudio and distributed under its own open-source license."
)

val EnglishStrings = Strings()

val PersianStrings = Strings(
    appName = "Aether Ng",
    appSubtitle = "کلاینت پیشرفته و امن تونل اتر ان‌جی",
    
    // Navigation
    tabDashboard = "داشبورد",
    tabSettings = "تنظیمات",
    tabLogs = "لاگ‌ها",
    tabAbout = "درباره ما",
    
    // Connection Status
    statusDisconnected = "قطع شده",
    statusConnecting = "در حال اتصال...",
    statusValidating = "اعتبارسنجی...",
    statusConnected = "متصل و ایمن",
    statusReconnecting = "اتصال مجدد...",
    statusStopping = "در حال قطع...",
    statusError = "خطای اتصال",
    
    // Actions
    actionConnect = "اتصال",
    actionDisconnect = "قطع اتصال",
    actionCancel = "انصراف",
    actionReset = "بازنشانی",
    actionSave = "ذخیره",
    actionApply = "اعمال",
    actionCopy = "کپی",
    actionShare = "اشتراک‌گذاری",
    actionDelete = "حذف",
    actionSearch = "جستجو",
    actionRestart = "راه‌اندازی مجدد",
    actionOptimize = "بهینه‌سازی",
    
    // Dashboard telemetry
    labelUplink = "ارسال (آپلود)",
    labelDownlink = "دریافت (دانلود)",
    labelSpeed = "سرعت",
    labelPing = "پینگ",
    labelLatency = "تأخیر",
    labelTraffic = "مصرف ترافیک",
    labelGateway = "گیت‌وی",
    labelServer = "سرور",
    labelIpAddress = "آدرس آی‌پی",
    labelDuration = "مدت زمان",
    
    // Settings Categories
    settingsTitle = "تنظیمات Aether Ng",
    settingsSubtitle = "پیکربندی پروتکل‌ها، فیلترگریز و انتقال داده",
    catSpeedTest = "تست سرعت اینترنت",
    catSpeedTestSub = "اندازه‌گیری دانلود، آپلود، پینگ و جیتر",
    catAutoDetect = "تشخیص خودکار هوشمند",
    catAutoDetectSub = "یافتن بهترین پروتکل و تنظیمات برای شبکه شما",
    catPresets = "پروفایل‌های پیکربندی",
    catPresetsSub = "پریست‌ها و تنظیمات دستی",
    catConnection = "اتصال و تونل‌سازی",
    catConnectionSub = "حالت اتصال، موتور تونل، تفکیک ترافیک برنامه‌ها",
    catProtocol = "پروتکل و انتقال",
    catProtocolSub = "ماسک (MASQUE)، H2، ECH، مبهم‌سازی، MTU",
    catZeroTrust = "کلودفلر زیرو تراست (Zero Trust)",
    catZeroTrustSub = "تیم، گیت‌وی و احراز هویت سازمانی",
    catNetwork = "پارامترهای شبکه",
    catNetworkSub = "پورت‌ها، SOCKS5، HTTP، دی‌ان‌اس (DNS)، پیر",
    catSecurity = "امنیت و پایداری",
    catSecuritySub = "کیل سوییچ، محافظت از نشت IPv6، اتصال مجدد هوشمند",
    catDiagnostics = "عیب‌یابی و هسته",
    catDiagnosticsSub = "سطوح لاگ‌گیری، پروفایل عملکرد، پروکسی بالادست",
    catHevEngine = "موتور بومی HEV",
    catHevEngineSub = "سطح لاگ، تایم‌اوت‌ها، حداکثر نشست (پیشرفته)",
    catSystem = "سیستم و نگهداری",
    catSystemSub = "تم برنامه، نسخه پشتیبان، بازیابی و بازنشانی",
    
    // Themes
    themeTitle = "تم برنامه",
    themeSystem = "پیش‌فرض سیستم",
    themeDark = "حالت تاریک",
    themeLight = "حالت روشن",
    
    // Backup & Reset
    backupExportTitle = "پشتیبان‌گیری کامل از تنظیمات",
    backupExportSub = "خروجی گرفتن از تمام تنظیمات در فایل .astf",
    backupImportTitle = "بازیابی از فایل پشتیبان",
    backupImportSub = "وارد کردن تنظیمات از فایل .astf",
    resetTitle = "بازنشانی به تنظیمات کارخانه",
    resetSub = "پاکسازی کلیه تنظیمات دستی و راه‌اندازی مجدد",

    // Dashboard Extra Strings
    tunnelTitle = "تانل Aether Ng",
    tunnelSubtitleTunnel = "تونل‌سازی امن و خصوصی",
    tunnelSubtitleProxy = "پروکسی محلی پرسرعت",
    statusProtectedConnected = "متصل و محافظت‌شده",
    statusProxyActive = "پروکسی فعال است",
    statusFindingServers = "در حال جستجوی سرورها...",
    statusEstablishingLink = "در حال برقراری اتصال...",
    statusReconnectingUpper = "در حال اتصال مجدد...",
    statusStoppingUpper = "در حال قطع اتصال...",
    statusErrorUpper = "خطای اتصال",
    statusReadyToConnect = "آماده برای اتصال",
    swipeToConnect = "برای اتصال بکشید",
    swipeToDisconnect = "برای قطع اتصال بکشید",
    releaseToDisconnect = "رها کنید تا قطع شود",
    startingTunnel = "در حال راه‌اندازی تونل...",
    swipeToRetry = "برای تلاش مجدد بکشید",
    labelUploadUpper = "ارسال (آپلود)",
    labelDownloadUpper = "دریافت (دانلود)",
    labelBypass = "بای‌پس",
    labelSpeedUpper = "سرعت",
    labelNetworkUpper = "شبکه",
    labelShowPublicIp = "نمایش آی‌پی عمومی",
    labelLocatingIp = "در حال یافتن آی‌پی...",
    labelCouldNotFindIp = "آی‌پی یافت نشد",
    labelNoUplink = "ارتباط برقرار نیست",
    labelLanguageTitle = "زبان برنامه (Language)",
    labelSupportTitle = "پشتیبانی از Aether Ng",
    labelSupportMessage = "برنامه Aether Ng یک پروژه رایگان و متن‌باز است که در اوقات فراغت توسعه می‌یابد.\nاگر برای شما مفید است، در کانال رسمی تلگرام ما عضو شوید تا آخرین آپدیت‌ها، قابلیت‌های جدید، رفع باگ‌ها و اطلاعیه‌های مهم را دریافت کنید.\nحمایت شما مایه دلگرمی ماست!",
    labelSupportJoin = "عضویت در کانال تلگرام",
    labelSupportDismiss = "متوجه شدم",
    connFailedMsg = "خطا در برقراری اتصال. لطفاً مجدداً تلاش کنید.",

    // Live Ping & Signal Visualizer
    livePingTitle = "پینگ و سیگنال زنده",
    liveSignalQuality = "سیگنال",
    labelLatencyUpper = "تاخیر (پینگ)",
    labelJitter = "نوسان (Jitter)",
    latencyOptimal = "عالی (سبز)",
    latencyGood = "مناسب (زرد)",
    latencyHigh = "بالا (قرمز)",
    latencyIdle = "آماده اتصال",
    tapToTestPing = "تست مجدد",

    // Speed Chart & Live Traffic
    speedChartTitle = "نمودار زنده سرعت و ترافیک",
    labelLiveSpeed = "سرعت زنده",
    labelTotalSent = "ارسال شده",
    labelTotalRecv = "دریافت شده",
    settingsShowLivePing = "نمایش ویجت پینگ و سیگنال زنده",
    settingsShowLivePingSub = "نمایش وضعیت پینگ و موج سیگنال در صفحه اصلی",
    settingsShowTrafficChart = "نمایش نمودار زنده سرعت و ترافیک",
    settingsShowTrafficChartSub = "نمایش نمودار گرافیکی موجی سرعت مصرفی در صفحه اصلی",

    // About
    aboutOverviewTitle = "نمای کلی پروژه",
    aboutCoreTitle = "هسته Aether",
    aboutHevTitle = "پشته بومی HEV",
    aboutBridgeTitle = "پل SocksTunBridge (کاتلین)",
    aboutDevLinksTitle = "لینک‌های توسعه و سورس",
    aboutTelegramTitle = "کانال تلگرام",
    aboutTelegramSub = "کانال تلگرام برای پشتیبانی، گفت‌وگو و اخبار توسعه.",
    aboutInstagramTitle = "ارتباط در اینستاگرام",
    aboutInstagramSub = "صفحه اینستاگرام Aliem061 برای اطلاعیه‌ها.",
    aboutAetherRepoTitle = "مخزن سورس Aether",
    aboutAetherRepoSub = "کد منبع و پیاده‌سازی پروتکل هسته برنامه.",
    aboutHevRepoTitle = "سورس پشته HEV",
    aboutHevRepoSub = "پیاده‌سازی بومی C برای پل ارتباطی TUN به ساکس.",
    aboutFooterBuiltWith = "Built with ",
    aboutFooterTeam = " by PowerSigma Team | ادیت شده توسط aliem",
    aboutFooterDisclaimer = "Aether Ng is an independent client project. Aether core is developed by CluvexStudio and distributed under its own open-source license."
)

val LocalAppStrings = staticCompositionLocalOf { EnglishStrings }

object AppStrings {
    val current: Strings
        @Composable
        @ReadOnlyComposable
        get() = LocalAppStrings.current
}
