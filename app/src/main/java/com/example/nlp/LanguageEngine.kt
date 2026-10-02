package com.example.nlp

import java.util.Locale

data class SupportedLanguage(
    val code: String,
    val name: String,
    val nativeName: String,
    val flag: String,
    val sampleCommand: String
)

sealed class AssistantIntent {
    data class OpenApp(val appQuery: String, val appName: String) : AssistantIntent()
    data class ToggleWifi(val enable: Boolean) : AssistantIntent()
    data class ToggleBluetooth(val enable: Boolean) : AssistantIntent()
    data class ToggleTorch(val enable: Boolean) : AssistantIntent()
    data class SetVolume(val percentage: Int) : AssistantIntent()
    data class CreateReminder(val title: String, val timeHint: String) : AssistantIntent()
    data class CreateNote(val content: String) : AssistantIntent()
    data class SearchFiles(val query: String) : AssistantIntent()
    object CheckSecurity : AssistantIntent()
    object CheckSystemStatus : AssistantIntent()
    data class RootShellCommand(val command: String) : AssistantIntent()
    data class Conversational(val query: String) : AssistantIntent()
}

data class IntentParseResult(
    val intent: AssistantIntent,
    val detectedLanguageCode: String,
    val spokenText: String,
    val isWakeWordDetected: Boolean
)

object LanguageEngine {

    val supportedLanguages = listOf(
        SupportedLanguage("en", "English", "English", "🇺🇸", "Open WhatsApp"),
        SupportedLanguage("bn", "Bengali", "বাংলা", "🇧🇩", "হোয়াটসঅ্যাপ খোলো"),
        SupportedLanguage("hi", "Hindi", "हिन्दी", "🇮🇳", "व्हाट्सएप खोलो"),
        SupportedLanguage("as", "Assamese", "অসমীয়া", "🇮🇳", "টৰ্চ জ্বলোৱা"),
        SupportedLanguage("ur", "Urdu", "اردو", "🇵🇰", "بلوٹوتھ آن کریں"),
        SupportedLanguage("ar", "Arabic", "العربية", "🇸🇦", "افتح الإعدادات"),
        SupportedLanguage("fr", "French", "Français", "🇫🇷", "Ouvre Chrome"),
        SupportedLanguage("de", "German", "Deutsch", "🇩🇪", "Schalte WLAN ein"),
        SupportedLanguage("es", "Spanish", "Español", "🇪🇸", "Abre la cámara"),
        SupportedLanguage("pt", "Portuguese", "Português", "🇧🇷", "Abrir configurações"),
        SupportedLanguage("ru", "Russian", "Русский", "🇷🇺", "Включи фонарик"),
        SupportedLanguage("ja", "Japanese", "日本語", "🇯🇵", "YouTubeを開いて"),
        SupportedLanguage("ko", "Korean", "한국어", "🇰🇷", "설정 열기"),
        SupportedLanguage("zh", "Chinese", "中文", "🇨🇳", "打开微信"),
        SupportedLanguage("it", "Italian", "Italiano", "🇮🇹", "Apri WhatsApp"),
        SupportedLanguage("tr", "Turkish", "Türkçe", "🇹🇷", "Ayarları aç"),
        SupportedLanguage("id", "Indonesian", "Bahasa Indonesia", "🇮🇩", "Buka YouTube")
    )

    fun detectLanguage(text: String, fallbackPrimary: String = "en"): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return fallbackPrimary

        // Check Unicode blocks
        var bengaliCount = 0
        var devanagariCount = 0
        var arabicCount = 0
        var cjkCount = 0
        var cyrillicCount = 0

        for (ch in trimmed) {
            val code = ch.code
            when {
                code in 0x0980..0x09FF -> bengaliCount++
                code in 0x0900..0x097F -> devanagariCount++
                code in 0x0600..0x06FF || code in 0x0750..0x077F -> arabicCount++
                code in 0x4E00..0x9FFF || code in 0x3040..0x309F || code in 0x30A0..0x30FF -> cjkCount++
                code in 0x0400..0x04FF -> cyrillicCount++
            }
        }

        if (bengaliCount > 1) return "bn"
        if (devanagariCount > 1) return "hi"
        if (arabicCount > 1) return "ar"
        if (cjkCount > 1) return "zh"
        if (cyrillicCount > 1) return "ru"

        // Romanized keywords for Hinglish / Banglish / Spanish / French
        val lower = trimmed.lowercase(Locale.ROOT)
        if (lower.contains("kholo") || lower.contains("chalu") || lower.contains("karo") ||
            lower.contains("band") || lower.contains("lagao") || lower.contains("batao")
        ) {
            return "hi"
        }
        if (lower.contains("khoro") || lower.contains("kholo") || lower.contains("chalao") ||
            lower.contains("bondho") || lower.contains("bolo") || lower.contains("dekhao")
        ) {
            return "bn"
        }
        if (lower.contains("ouvre") || lower.contains("active") || lower.contains("merci")) return "fr"
        if (lower.contains("abre") || lower.contains("apaga") || lower.contains("enciende")) return "es"
        if (lower.contains("öffne") || lower.contains("schalte") || lower.contains("bitte")) return "de"

        return fallbackPrimary
    }

    fun parseInput(
        rawText: String,
        wakeWord: String,
        assistantName: String,
        configuredPrimaryLang: String
    ): IntentParseResult {
        var text = rawText.trim()
        val lowerRaw = text.lowercase(Locale.ROOT)
        val lowerWake = wakeWord.lowercase(Locale.ROOT)
        val lowerName = assistantName.lowercase(Locale.ROOT)

        var wakeWordDetected = false
        if (lowerRaw.startsWith(lowerWake)) {
            wakeWordDetected = true
            text = text.substring(wakeWord.length).trim()
        } else if (lowerRaw.startsWith(lowerName)) {
            wakeWordDetected = true
            text = text.substring(assistantName.length).trim()
        } else if (lowerRaw.contains(lowerWake) || lowerRaw.contains(lowerName)) {
            wakeWordDetected = true
        }

        // Clean leading punctuation
        text = text.removePrefix(",").removePrefix(":").trim()
        val lang = detectLanguage(text, configuredPrimaryLang)
        val lower = text.lowercase(Locale.ROOT)

        val intent: AssistantIntent = when {
            // Root Shell Command
            lower.startsWith("root:") || lower.startsWith("su ") || lower.contains("run root") ||
            lower.contains("shell execute") -> {
                val cmd = text.substringAfter("root:").substringAfter("su ").trim()
                AssistantIntent.RootShellCommand(if (cmd.isNotEmpty()) cmd else "id")
            }

            // Wi-Fi
            (lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("ওয়াইফাই") || lower.contains("वाईफाई")) &&
            (lower.contains("off") || lower.contains("band") || lower.contains("bondho") || lower.contains("desactivar") || lower.contains("বন্ধ")) -> {
                AssistantIntent.ToggleWifi(false)
            }
            (lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("ওয়াইফাই") || lower.contains("वाईफाई")) &&
            (lower.contains("on") || lower.contains("chalu") || lower.contains("kholo") || lower.contains("চালু") || lower.contains("activa") || lower.contains("चालू")) -> {
                AssistantIntent.ToggleWifi(true)
            }

            // Bluetooth
            (lower.contains("bluetooth") || lower.contains("ব্লুটুথ") || lower.contains("ब्लूटूथ")) &&
            (lower.contains("off") || lower.contains("band") || lower.contains("bondho") || lower.contains("desactivar") || lower.contains("বন্ধ")) -> {
                AssistantIntent.ToggleBluetooth(false)
            }
            (lower.contains("bluetooth") || lower.contains("ব্লুটুথ") || lower.contains("ब्लूटूथ")) &&
            (lower.contains("on") || lower.contains("chalu") || lower.contains("चालू") || lower.contains("চালু") || lower.contains("activa")) -> {
                AssistantIntent.ToggleBluetooth(true)
            }

            // Torch / Flashlight
            (lower.contains("torch") || lower.contains("flashlight") || lower.contains("টর্চ") || lower.contains("टॉर्च") || lower.contains("লাইট")) &&
            (lower.contains("off") || lower.contains("band") || lower.contains("bondho") || lower.contains("apaga") || lower.contains("বন্ধ")) -> {
                AssistantIntent.ToggleTorch(false)
            }
            (lower.contains("torch") || lower.contains("flashlight") || lower.contains("টর্চ") || lower.contains("टॉर्च") || lower.contains("লাইট") || lower.contains("jalau")) &&
            (lower.contains("on") || lower.contains("chalu") || lower.contains("jalo") || lower.contains("चालू") || lower.contains("জ্বালো") || lower.contains("activa")) -> {
                AssistantIntent.ToggleTorch(true)
            }

            // Volume
            lower.contains("volume") || lower.contains("आवाज") || lower.contains("আওয়াজ") || lower.contains("sonido") -> {
                val percentage = when {
                    lower.contains("max") || lower.contains("full") || lower.contains("100") -> 100
                    lower.contains("mute") || lower.contains("silent") || lower.contains("0") -> 0
                    lower.contains("half") || lower.contains("50") -> 50
                    lower.contains("increase") || lower.contains("barhao") || lower.contains("up") || lower.contains("বাড়িয়ে") -> 85
                    lower.contains("decrease") || lower.contains("down") || lower.contains("kam") || lower.contains("কমিয়ে") -> 30
                    else -> 70
                }
                AssistantIntent.SetVolume(percentage)
            }

            // Security check
            lower.contains("security") || lower.contains("নিরাপত্তা") || lower.contains("सुरक्षा") ||
            lower.contains("biometric") || lower.contains("shield") || lower.contains("status") -> {
                AssistantIntent.CheckSecurity
            }

            // Battery / System check
            lower.contains("battery") || lower.contains("storage") || lower.contains("ব্যাটারি") ||
            lower.contains("बैटरी") || lower.contains("memory status") || lower.contains("device status") -> {
                AssistantIntent.CheckSystemStatus
            }

            // Reminder
            lower.contains("remind") || lower.contains("alarm") || lower.contains("মনে করিয়ে") ||
            lower.contains("याद दिलाओ") || lower.contains("রেকর্ড") || lower.contains("recordatorio") -> {
                val cleaned = text.replace("remind me to", "", true)
                    .replace("remind me", "", true)
                    .replace("মনে করিয়ে দাও", "", true)
                    .replace("याद दिलाना", "", true)
                    .trim()
                AssistantIntent.CreateReminder(
                    title = if (cleaned.isNotEmpty()) cleaned else "Scheduled Task",
                    timeHint = "Today"
                )
            }

            // Note
            lower.contains("note") || lower.contains("নোট") || lower.contains("लिखो") ||
            lower.contains("नया नोट") || lower.contains("take a note") || lower.contains("লিখো") -> {
                val noteBody = text.replace("note:", "", true)
                    .replace("take a note", "", true)
                    .replace("নোট লেখো", "", true)
                    .replace("नोट बनाओ", "", true)
                    .trim()
                AssistantIntent.CreateNote(if (noteBody.isNotEmpty()) noteBody else "Quick note")
            }

            // Search Files
            lower.contains("find file") || lower.contains("search photo") || lower.contains("document") ||
            lower.contains("ফাইল খোঁজো") || lower.contains("ছবি খোঁজো") || lower.contains("ढूंढो") -> {
                AssistantIntent.SearchFiles(text)
            }

            // Open App (handles multilingual + Hinglish/Banglish patterns)
            detectAppOpenIntent(lower, text) != null -> {
                detectAppOpenIntent(lower, text)!!
            }

            else -> AssistantIntent.Conversational(text)
        }

        return IntentParseResult(
            intent = intent,
            detectedLanguageCode = lang,
            spokenText = text,
            isWakeWordDetected = wakeWordDetected
        )
    }

    private fun detectAppOpenIntent(lower: String, original: String): AssistantIntent.OpenApp? {
        val appList = listOf(
            "whatsapp" to "WhatsApp",
            "youtube" to "YouTube",
            "chrome" to "Google Chrome",
            "google" to "Google",
            "maps" to "Google Maps",
            "camera" to "Camera",
            "settings" to "Device Settings",
            "spotify" to "Spotify",
            "gmail" to "Gmail",
            "telegram" to "Telegram",
            "calculator" to "Calculator",
            "gallery" to "Gallery / Photos",
            "photos" to "Google Photos",
            "instagram" to "Instagram",
            "facebook" to "Facebook",
            "twitter" to "X / Twitter",
            "phone" to "Phone",
            "contacts" to "Contacts",
            "calendar" to "Calendar"
        )

        // Verbs: open, launch, kholo, chalu, khulchi, open karo, abre, ouvre, öffne, खोलो, খুলুন, খুলছি
        val triggers = listOf(
            "open", "launch", "start", "kholo", "chalu", "karo", "chalao", "abre",
            "ouvre", "öffne", "खोलो", "खोलिए", "খুলুন", "খোলো", "খুলছি", "চালু করো"
        )

        val containsTrigger = triggers.any { lower.contains(it) }

        for ((key, name) in appList) {
            if (lower.contains(key)) {
                if (containsTrigger || lower.startsWith(key)) {
                    return AssistantIntent.OpenApp(key, name)
                }
            }
        }
        return null
    }

    fun generateResponse(
        intent: AssistantIntent,
        language: String,
        assistantName: String,
        personality: String,
        ownerName: String
    ): String {
        val pPrefix = when (personality) {
            "GUARDIAN" -> "[Shield Active] "
            "TECHNICAL" -> "[Local Kernel] "
            "CASUAL" -> ""
            "FRIENDLY" -> ""
            else -> ""
        }

        return when (language) {
            "bn" -> when (intent) {
                is AssistantIntent.OpenApp -> "${pPrefix}${intent.appName} খুলছি, $ownerName।"
                is AssistantIntent.ToggleWifi -> if (intent.enable) "${pPrefix}ওয়াইফাই চালু করা হয়েছে।" else "${pPrefix}ওয়াইফাই বন্ধ করা হয়েছে।"
                is AssistantIntent.ToggleBluetooth -> if (intent.enable) "${pPrefix}ব্লুটুথ সক্রিয় করা হলো।" else "${pPrefix}ব্লুটুথ নিষ্ক্রিয় করা হলো।"
                is AssistantIntent.ToggleTorch -> if (intent.enable) "${pPrefix}টর্চ চালু করা হলো।" else "${pPrefix}টর্চ বন্ধ করা হলো।"
                is AssistantIntent.SetVolume -> "${pPrefix}আওয়াজ ${intent.percentage}% এ নির্ধারণ করা হলো।"
                is AssistantIntent.CreateReminder -> "${pPrefix}রিমাইন্ডার সংরক্ষিত: '${intent.title}'।"
                is AssistantIntent.CreateNote -> "${pPrefix}স্থানীয় মেমোরিতে নোট সংরক্ষিত হলো।"
                is AssistantIntent.SearchFiles -> "${pPrefix}ডিভাইসে ফাইল অনুসন্ধান করছি।"
                is AssistantIntent.CheckSecurity -> "${pPrefix}বায়োমেট্রিক শিল্ড সক্রিয়। শুধুমাত্র মালিকের ভয়েস এবং মুখের স্বীকৃতি গ্রহণযোগ্য।"
                is AssistantIntent.CheckSystemStatus -> "${pPrefix}স্থানীয় সিস্টেম সুরক্ষিত। ব্যাটারি এবং মেমোরি সন্তোষজনক।"
                is AssistantIntent.RootShellCommand -> "${pPrefix}রুট কমান্ড অনুমোদিত এবং কার্যকর করা হচ্ছে।"
                is AssistantIntent.Conversational -> "${pPrefix}আমি $assistantName, আপনার ব্যক্তিগত অফলাইন এআই সহকারী। কীভাবে সাহায্য করতে পারি, $ownerName?"
            }

            "hi" -> when (intent) {
                is AssistantIntent.OpenApp -> "${pPrefix}${intent.appName} खोल रहा हूँ, $ownerName।"
                is AssistantIntent.ToggleWifi -> if (intent.enable) "${pPrefix}वाई-फाई चालू कर दिया गया है।" else "${pPrefix}वाई-फाई बंद कर दिया गया है।"
                is AssistantIntent.ToggleBluetooth -> if (intent.enable) "${pPrefix}ब्लूटूथ चालू कर रहा हूँ।" else "${pPrefix}ब्लूटूथ बंद कर दिया गया है।"
                is AssistantIntent.ToggleTorch -> if (intent.enable) "${pPrefix}टॉर्च चालू कर दी गई है।" else "${pPrefix}टॉर्च बंद कर दी गई है।"
                is AssistantIntent.SetVolume -> "${pPrefix}आवाज़ ${intent.percentage}% पर सेट कर दी गई है।"
                is AssistantIntent.CreateReminder -> "${pPrefix}रिमाइंडर सेट किया गया: '${intent.title}'।"
                is AssistantIntent.CreateNote -> "${pPrefix}सुरक्षित लोकल मेमोरी में नोट सहेज लिया गया है।"
                is AssistantIntent.SearchFiles -> "${pPrefix}स्थानीय डिवाइस में फ़ाइलें खोजी जा रही हैं।"
                is AssistantIntent.CheckSecurity -> "${pPrefix}बायोमेट्रिक सुरक्षा सक्रिय है। केवल मालिक $ownerName अधिकृत हैं।"
                is AssistantIntent.CheckSystemStatus -> "${pPrefix}लोकल सिस्टम और मेमोरी स्थिति सामान्य और सुरक्षित है।"
                is AssistantIntent.RootShellCommand -> "${pPrefix}रूट कमांड निष्पादित की जा रही है।"
                is AssistantIntent.Conversational -> "${pPrefix}मैं $assistantName हूँ, आपका निजी ऑन-डिवाइस एआई सहायक। आज मैं आपकी क्या मदद कर सकता हूँ?"
            }

            "ur" -> when (intent) {
                is AssistantIntent.OpenApp -> "${intent.appName} کھول رہا ہوں، $ownerName۔"
                is AssistantIntent.ToggleWifi -> "وائی فائی سیٹنگز کو اپ ڈیٹ کر دیا گیا ہے۔"
                is AssistantIntent.ToggleBluetooth -> "بلوٹوتھ کو فعال کر دیا گیا ہے۔"
                is AssistantIntent.ToggleTorch -> "ٹارچ آن کر دی گئی ہے۔"
                else -> "میں $assistantName ہوں، آپ کا ذاتی اسسٹنٹ۔"
            }

            "as" -> when (intent) {
                is AssistantIntent.OpenApp -> "${intent.appName} খুলি থকা হৈছে, $ownerName।"
                is AssistantIntent.ToggleWifi -> "ৱাই-ফাই স্থিতি সলনি কৰা হৈছে।"
                is AssistantIntent.ToggleTorch -> "টৰ্চ জ্বলোৱা হৈছে।"
                else -> "মই আপোনাৰ ব্যক্তিগত সহায়ক $assistantName।"
            }

            "es" -> when (intent) {
                is AssistantIntent.OpenApp -> "${pPrefix}Abriendo ${intent.appName}, $ownerName."
                is AssistantIntent.ToggleWifi -> if (intent.enable) "${pPrefix}Wi-Fi activado." else "${pPrefix}Wi-Fi desactivado."
                is AssistantIntent.ToggleBluetooth -> if (intent.enable) "${pPrefix}Bluetooth activado." else "${pPrefix}Bluetooth desactivado."
                is AssistantIntent.ToggleTorch -> if (intent.enable) "${pPrefix}Linterna encendida." else "${pPrefix}Linterna apagada."
                is AssistantIntent.SetVolume -> "${pPrefix}Volumen ajustado al ${intent.percentage}%."
                is AssistantIntent.CreateReminder -> "${pPrefix}Recordatorio guardado: '${intent.title}'."
                is AssistantIntent.CreateNote -> "${pPrefix}Nota almacenada en memoria local encriptada."
                is AssistantIntent.CheckSecurity -> "${pPrefix}Protección biométrica activa. Acceso verificado para $ownerName."
                else -> "${pPrefix}Soy $assistantName, tu asistente de IA personal y seguro."
            }

            "fr" -> when (intent) {
                is AssistantIntent.OpenApp -> "${pPrefix}Ouverture de ${intent.appName}."
                is AssistantIntent.ToggleWifi -> if (intent.enable) "${pPrefix}Wi-Fi activé." else "${pPrefix}Wi-Fi désactivé."
                is AssistantIntent.ToggleTorch -> if (intent.enable) "${pPrefix}Lampe de poche allumée." else "${pPrefix}Lampe éteinte."
                else -> "${pPrefix}Bonjour $ownerName, je suis $assistantName, votre assistant sécurisé."
            }

            else -> when (intent) {
                is AssistantIntent.OpenApp -> "${pPrefix}Opening ${intent.appName}, $ownerName."
                is AssistantIntent.ToggleWifi -> if (intent.enable) "${pPrefix}Wi-Fi has been enabled." else "${pPrefix}Wi-Fi has been disabled."
                is AssistantIntent.ToggleBluetooth -> if (intent.enable) "${pPrefix}Bluetooth has been enabled." else "${pPrefix}Bluetooth has been disabled."
                is AssistantIntent.ToggleTorch -> if (intent.enable) "${pPrefix}Flashlight switched ON." else "${pPrefix}Flashlight switched OFF."
                is AssistantIntent.SetVolume -> "${pPrefix}Volume set to ${intent.percentage}%."
                is AssistantIntent.CreateReminder -> "${pPrefix}Reminder saved: '${intent.title}'."
                is AssistantIntent.CreateNote -> "${pPrefix}Note stored locally in encrypted memory vault."
                is AssistantIntent.SearchFiles -> "${pPrefix}Searching local documents and media securely."
                is AssistantIntent.CheckSecurity -> "${pPrefix}Aegis Shield active: Owner biometrics verified, voice signature enforced, local storage encrypted."
                is AssistantIntent.CheckSystemStatus -> "${pPrefix}System optimal. Offline AI neural engine running with zero telemetry."
                is AssistantIntent.RootShellCommand -> "${pPrefix}Executing approved root-level shell command with owner elevation."
                is AssistantIntent.Conversational -> when (personality) {
                    "GUARDIAN" -> "I am $assistantName. All systems operational and guarded. How may I secure your workflow, $ownerName?"
                    "TECHNICAL" -> "$assistantName ready. Memory index synced. Awaiting instructions, $ownerName."
                    "FRIENDLY" -> "Hello $ownerName! Great to assist you today. What can I do for you?"
                    "CASUAL" -> "Hey $ownerName! What's up? Ready when you are."
                    else -> "Hello $ownerName, I am $assistantName, your personal AI assistant. How can I help you today?"
                }
            }
        }
    }
}
