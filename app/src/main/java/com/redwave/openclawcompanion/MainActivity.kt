package com.redwave.openclawcompanion

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val bg = Color.rgb(10, 10, 13)
    private val panel = Color.rgb(23, 23, 29)
    private val red = Color.rgb(255, 55, 70)
    private val white = Color.rgb(245, 245, 248)
    private val muted = Color.rgb(160, 160, 172)
    private lateinit var messages: LinearLayout
    private lateinit var input: EditText
    private lateinit var scroll: ScrollView
    private lateinit var status: TextView
    private val executor = Executors.newSingleThreadExecutor()
    private val history = JSONArray()
    private val prefs by lazy { getSharedPreferences("assistant_prefs", Context.MODE_PRIVATE) }
    private val speechRequest = 501

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        buildUi()
        addMessage("assistant", "नमस्ते! मैं आपका OpenClaw Companion हूँ। मैं चैट कर सकता हूँ और कुछ सुरक्षित फोन actions कर सकता हूँ।\n\nTry: “YouTube खोलो”, “वेब पर Android सीखना खोजो”, “मदद”")
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(10))
            setBackgroundColor(bg)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val logo = TextView(this).apply {
            text = "◉"
            textSize = 30f
            setTextColor(red)
            setPadding(0, 0, dp(10), 0)
        }
        val titleCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titleCol.addView(TextView(this).apply {
            text = "OPENCLAW COMPANION"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(white)
        })
        titleCol.addView(TextView(this).apply {
            text = "ANDROID AI • LOCAL ACTIONS"
            textSize = 10f
            setTextColor(muted)
        })
        header.addView(logo)
        header.addView(titleCol, LinearLayout.LayoutParams(0, -2, 1f))
        val settings = TextView(this).apply {
            text = "⚙"
            textSize = 25f
            setTextColor(white)
            setPadding(dp(12), dp(6), dp(6), dp(6))
            setOnClickListener { showSettings() }
        }
        header.addView(settings)
        root.addView(header)

        status = TextView(this).apply {
            text = "● READY  •  Free mode"
            textSize = 11f
            setTextColor(Color.rgb(105, 220, 145))
            setPadding(dp(2), dp(10), 0, dp(10))
        }
        root.addView(status)

        scroll = ScrollView(this).apply { isFillViewport = true }
        messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, dp(12))
        }
        scroll.addView(messages)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val quick = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        quick.addView(chip("Help") { handleUserText("मदद") }, LinearLayout.LayoutParams(0, dp(38), 1f))
        quick.addView(chip("Open YouTube") { handleUserText("YouTube खोलो") }, LinearLayout.LayoutParams(0, dp(38), 1f).apply { leftMargin = dp(6) })
        quick.addView(chip("Web search") { handleUserText("वेब पर Android सीखना खोजो") }, LinearLayout.LayoutParams(0, dp(38), 1f).apply { leftMargin = dp(6) })
        root.addView(quick)
        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(5), dp(5), dp(5))
            background = rounded(panel, 18)
        }
        input = EditText(this).apply {
            hint = "Message your assistant…"
            setHintTextColor(muted)
            setTextColor(white)
            textSize = 15f
            maxLines = 4
            setSingleLine(false)
            imeOptions = EditorInfo.IME_ACTION_SEND
            background = null
            setPadding(dp(8), dp(8), dp(4), dp(8))
            setOnEditorActionListener { _, action, _ ->
                if (action == EditorInfo.IME_ACTION_SEND) { sendCurrent(); true } else false
            }
        }
        composer.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        val mic = TextView(this).apply {
            text = "🎙"
            textSize = 19f
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setOnClickListener { startVoiceInput() }
        }
        composer.addView(mic)
        val send = TextView(this).apply {
            text = "➤"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(red, 14)
            setPadding(dp(14), dp(9), dp(14), dp(9))
            setOnClickListener { sendCurrent() }
        }
        composer.addView(send)
        val composerParams = LinearLayout.LayoutParams(-1, -2)
        composerParams.topMargin = dp(9)
        root.addView(composer, composerParams)
        setContentView(root)
    }

    private fun chip(label: String, action: () -> Unit): TextView = TextView(this).apply {
        text = label
        gravity = Gravity.CENTER
        textSize = 11f
        setTextColor(white)
        background = rounded(panel, 12)
        setOnClickListener { action() }
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun addMessage(who: String, text: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = if (who == "user") Gravity.END else Gravity.START
            setPadding(0, dp(5), 0, dp(5))
        }
        val label = TextView(this).apply {
            this.text = if (who == "user") "YOU" else "COMPANION"
            textSize = 9f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (who == "user") red else muted)
            setPadding(dp(5), 0, dp(5), dp(4))
        }
        val bubble = TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(white)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(if (who == "user") Color.rgb(65, 24, 32) else panel, 14)
        }
        row.addView(label)
        row.addView(bubble)
        messages.addView(row, LinearLayout.LayoutParams(-1, -2))
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun sendCurrent() {
        val text = input.text.toString().trim()
        if (text.isEmpty()) return
        input.setText("")
        handleUserText(text)
    }

    private fun handleUserText(text: String) {
        addMessage("user", text)
        val local = runLocalAction(text)
        if (local != null) {
            addMessage("assistant", local)
            return
        }
        val apiKey = prefs.getString("gemini_key", "") ?: ""
        if (apiKey.isBlank()) {
            addMessage("assistant", freeReply(text))
            return
        }
        status.text = "● THINKING • Gemini API"
        executor.execute {
            val answer = try { callGemini(apiKey, text) } catch (e: Exception) {
                "AI कनेक्शन नहीं हो पाया: ${e.message ?: "network error"}\nSettings में API key और इंटरनेट जाँचें।"
            }
            runOnUiThread {
                status.text = "● READY • " + if (apiKey.isBlank()) "Free mode" else "Gemini API"
                addMessage("assistant", answer)
            }
        }
    }

    private fun runLocalAction(text: String): String? {
        val q = text.lowercase(Locale.ROOT)
        if (q.contains("मदद") || q == "help" || q.contains("क्या कर")) {
            return "मैं ये कर सकता हूँ:\n• YouTube खोलो\n• Google पर कुछ खोजो\n• Settings खोलो\n• तारीख/समय बताओ\n• नोट बनाने के लिए टेक्स्ट तैयार करो\n\nसामान्य AI चैट के लिए Settings में Gemini API key जोड़ें। Free tier उपलब्धता Google की शर्तों पर निर्भर है।"
        }
        if (q.contains("youtube") && (q.contains("खोल") || q.contains("open"))) {
            openUrl("https://www.youtube.com")
            return "YouTube खोलने की कोशिश की।"
        }
        if ((q.contains("वेब पर") || q.contains("google") || q.contains("खोजो") || q.contains("search")) && q.length > 8) {
            val query = text.replace(Regex("(?i)वेब पर|google पर|खोजो|search for|search"), "").trim()
            openUrl("https://www.google.com/search?q=" + Uri.encode(query.ifBlank { text }))
            return "ब्राउज़र में वेब सर्च खोल दिया।"
        }
        if (q.contains("settings") || q.contains("सेटिंग")) {
            try { startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)) }
            catch (_: Exception) { return "फोन Settings नहीं खुल सकी।" }
            return "फोन की Settings खोल दी।"
        }
        if (q.contains("समय") || q.contains("time") || q.contains("तारीख") || q.contains("date")) {
            return "फोन के अनुसार अभी: " + java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(java.util.Date())
        }
        if (q.startsWith("note:") || q.startsWith("नोट:")) {
            val note = text.substringAfter(":").trim()
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"; putExtra(Intent.EXTRA_TEXT, note); putExtra(Intent.EXTRA_TITLE, "Assistant note")
            }
            startActivity(Intent.createChooser(intent, "नोट सेव/शेयर करें"))
            return "नोट को सेव करने या शेयर करने के लिए Android share menu खोल दिया।"
        }
        return null
    }

    private fun freeReply(text: String): String {
        val q = text.lowercase(Locale.ROOT)
        return when {
            q.contains("hello") || q.contains("नमस्ते") || q.contains("hi") ->
                "नमस्ते! 😊 मैं Free mode में हूँ। फोन के कुछ actions कर सकता हूँ। पूरी AI बातचीत के लिए Settings में Gemini API key जोड़ सकते हो।"
            q.contains("तुम कौन") || q.contains("who are you") ->
                "मैं OpenClaw Companion का Android starter assistant हूँ। अभी local commands बिना API के चलती हैं; Gemini API जोड़ने पर AI chat चालू होती है।"
            else ->
                "मैंने तुम्हारा संदेश समझा: “$text”\n\nFree local mode में सामान्य AI मॉडल शामिल नहीं है। बिना अपने सर्वर के AI chat चालू करने के लिए ⚙ Settings में अपनी Gemini API key जोड़ो। API key ऐप में स्थानीय रूप से रखी जाएगी; इसे किसी के साथ शेयर मत करना।"
        }
    }

    private fun openUrl(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (_: Exception) { Toast.makeText(this, "कोई browser उपलब्ध नहीं है", Toast.LENGTH_SHORT).show() }
    }

    private fun startVoiceInput() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "बोलिए…")
            }
            startActivityForResult(intent, speechRequest)
        } catch (_: Exception) {
            Toast.makeText(this, "Voice input उपलब्ध नहीं है", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Deprecated in Android API; kept compatible with API 26+")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == speechRequest && resultCode == Activity.RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!result.isNullOrBlank()) { input.setText(result); sendCurrent() }
        }
    }

    private fun showSettings() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            setBackgroundColor(bg)
        }
        box.addView(TextView(this).apply {
            text = "AI SETTINGS"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(red)
        })
        box.addView(TextView(this).apply {
            text = "Gemini API key (optional). Get a key from Google AI Studio. The app calls Gemini directly; no custom backend is used."
            textSize = 13f
            setTextColor(muted)
            setPadding(0, dp(8), 0, dp(10))
        })
        val keyInput = EditText(this).apply {
            hint = "Paste API key"
            setHintTextColor(muted)
            setTextColor(white)
            setSingleLine(true)
            inputType = 0x00000081
            setText(prefs.getString("gemini_key", ""))
            background = rounded(panel, 10)
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        box.addView(keyInput)
        val save = Button(this).apply {
            text = "SAVE KEY"
            setTextColor(Color.WHITE)
            setBackgroundColor(red)
            setOnClickListener {
                prefs.edit().putString("gemini_key", keyInput.text.toString().trim()).apply()
                status.text = if (keyInput.text.toString().isBlank()) "● READY • Free mode" else "● READY • Gemini API"
                Toast.makeText(this@MainActivity, "Settings saved on this phone", Toast.LENGTH_SHORT).show()
                (box.parent as? android.view.ViewGroup)?.let { }
            }
        }
        box.addView(save)
        AlertDialogCompat.show(this, box)
    }

    private fun callGemini(apiKey: String, prompt: String): String {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + Uri.encode(apiKey))
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 30000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        val body = JSONObject()
            .put("system_instruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", "You are a helpful Android AI companion. Reply in the language the user uses, especially Hindi or Hinglish. You may suggest Android actions but never claim an action occurred unless the app did it."))))
            .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
            .put("generationConfig", JSONObject().put("temperature", 0.7).put("maxOutputTokens", 900))
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val response = stream.bufferedReader().use { it.readText() }
        conn.disconnect()
        if (code !in 200..299) throw Exception("HTTP $code")
        val json = JSONObject(response)
        return json.getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
    }
}

private object AlertDialogCompat {
    fun show(activity: Activity, content: View) {
        android.app.AlertDialog.Builder(activity)
            .setView(content)
            .setNegativeButton("CLOSE", null)
            .show()
    }
}
