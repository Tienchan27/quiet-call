package vn.quietcall

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.text.InputType
import android.text.InputFilter
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.*
import vn.quietcall.data.QuietRepository
import vn.quietcall.data.HistoryUpdates
import vn.quietcall.data.ImportPreview
import vn.quietcall.domain.*
import vn.quietcall.screening.Contacts
import vn.quietcall.ui.Glyph
import vn.quietcall.ui.QuietIcon
import vn.quietcall.ui.ShieldScene
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private lateinit var repository: QuietRepository
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var nav: LinearLayout
    private val io = Executors.newSingleThreadExecutor()
    private var page = 0
    private var search = ""
    private var onlyBlocked = false
    private var historyFilter = 0
    private var historyQuery = ""
    private var historyPageIndex = 0
    private var brandPageIndex = 0
    private var importing = false
    private var dataWorkTitle = "Đang xử lý dữ liệu"
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingSearch: Runnable? = null
    private val historyObserver: () -> Unit = {
        if (!isDestroyed && !importing && currentFocus !is EditText && (page == 0 || page == 2)) render()
    }
    private val ink = Color.rgb(22, 46, 40)
    private val muted = Color.rgb(84, 105, 96)
    private val green = Color.rgb(20, 125, 104)
    private val canvasColor = Color.rgb(243, 246, 248)
    private val pale = Color.rgb(226, 240, 231)
    private val red = Color.rgb(169, 63, 63)
    private val violet = Color.rgb(105, 86, 172)
    private val amber = Color.rgb(145, 99, 31)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        page = savedInstanceState?.getInt("page") ?: 0
        search = savedInstanceState?.getString("search").orEmpty()
        historyQuery = savedInstanceState?.getString("historyQuery").orEmpty()
        historyFilter = savedInstanceState?.getInt("historyFilter") ?: 0
        onlyBlocked = savedInstanceState?.getBoolean("onlyBlocked") ?: false
        repository = QuietRepository(this)
        root = column().apply { setBackgroundColor(canvasColor) }
        root.setOnApplyWindowInsetsListener { view, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.ime())
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            } else {
                @Suppress("DEPRECATION")
                view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            }
            insets
        }
        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false }
        content = column().apply { setPadding(dp(20), dp(18), dp(20), dp(24)) }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        nav = row().apply { setPadding(dp(8), dp(8), dp(8), dp(8)); setBackgroundColor(Color.WHITE) }
        root.addView(nav)
        setContentView(root)
        root.requestApplyInsets()
    }
    override fun onResume() { super.onResume(); if (::content.isInitialized) render() }
    override fun onStart() {
        super.onStart()
        HistoryUpdates.addObserver(historyObserver)
    }
    override fun onStop() { HistoryUpdates.removeObserver(historyObserver); super.onStop() }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("page", page); outState.putString("search", search); outState.putString("historyQuery", historyQuery)
        outState.putInt("historyFilter", historyFilter); outState.putBoolean("onlyBlocked", onlyBlocked)
        super.onSaveInstanceState(outState)
    }
    override fun onDestroy() { pendingSearch?.let(mainHandler::removeCallbacks); io.shutdown(); repository.close(); super.onDestroy() }

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun shape(color: Int, radius: Int = 18, stroke: Boolean = false) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
        if (stroke) setStroke(dp(1), Color.rgb(219, 227, 221))
    }
    private fun label(text: String, size: Float = 14f, color: Int = ink, bold: Boolean = false) = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setLineSpacing(dp(3).toFloat(), 1f)
    }
    private fun LinearLayout.gap(height: Int = 12) { addView(View(this@MainActivity), LinearLayout.LayoutParams(1, dp(height))) }
    private fun card(parent: LinearLayout = content, color: Int = Color.WHITE): LinearLayout = column().apply {
        background = shape(color, 22); setPadding(dp(16), dp(16), dp(16), dp(16))
        parent.addView(this, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
    }
    private fun icon(glyph: Glyph, color: Int = green, size: Int = 22) = QuietIcon(this, glyph, color).apply { layoutParams = LinearLayout.LayoutParams(dp(size), dp(size)) }
    private fun iconTile(glyph: Glyph, tint: Int = green, bg: Int = pale, size: Int = 42): FrameLayout = FrameLayout(this).apply {
        background = shape(bg, 14)
        addView(QuietIcon(this@MainActivity, glyph, tint), FrameLayout.LayoutParams(dp(size / 2), dp(size / 2), Gravity.CENTER))
        layoutParams = LinearLayout.LayoutParams(dp(size), dp(size))
    }
    private fun iconButton(glyph: Glyph, description: String, tint: Int = green, bg: Int = pale, action: () -> Unit): FrameLayout = iconTile(glyph, tint, bg, 48).apply {
        contentDescription = description; tooltipText = description; importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        background = RippleDrawable(ColorStateList.valueOf(Color.argb(35, 20, 125, 104)), shape(bg, 14), shape(Color.WHITE, 14))
        isFocusable = true; setOnClickListener { action() }
    }
    private fun badge(text: String, tint: Int = green, bg: Int = pale) = label(text, 11f, tint, true).apply {
        background = shape(bg, 9); setPadding(dp(9), dp(5), dp(9), dp(5)); gravity = Gravity.CENTER
    }
    private fun oneLine(text: String, size: Float = 13f, color: Int = muted, bold: Boolean = false) = label(text, size, color, bold).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END }
    private fun section(title: String, actionText: String? = null, action: (() -> Unit)? = null) {
        val heading = row()
        heading.addView(label(title, 17f, ink, true), LinearLayout.LayoutParams(0, -2, 1f))
        if (actionText != null && action != null) heading.addView(label(actionText, 12f, green, true).apply { gravity = Gravity.CENTER; minHeight = dp(48); setPadding(dp(10), 0, 0, 0); setOnClickListener { action() } })
        content.addView(heading); content.gap(8)
    }
    private fun settingEntry(parent: LinearLayout, glyph: Glyph, title: String, subtitle: String, tint: Int = green, action: () -> Unit) {
        val entry = row().apply { setPadding(0, dp(10), 0, dp(10)); minimumHeight = dp(64); setOnClickListener { action() }; isFocusable = true; background = RippleDrawable(ColorStateList.valueOf(Color.argb(20, 20, 125, 104)), null, shape(Color.WHITE, 12)) }
        entry.addView(iconTile(glyph, tint, if (tint == red) Color.rgb(251, 235, 235) else if (tint == violet) Color.rgb(240, 236, 250) else pale, 38))
        val text = column().apply { addView(label(title, 15f, ink, true)); gap(2); addView(oneLine(subtitle, 12f)) }
        entry.addView(text, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(12); marginEnd = dp(8) })
        entry.addView(icon(Glyph.CHEVRON, muted, 16)); parent.addView(entry)
    }
    private fun searchBox(hint: String, value: String): Pair<LinearLayout, EditText> {
        val input = field(hint, value).apply { background = null; setPadding(dp(10), dp(12), 0, dp(12)); textSize = 14f }
        val box = row().apply { background = shape(Color.WHITE, 16, true); setPadding(dp(14), 0, dp(4), 0) }
        box.addView(icon(Glyph.SEARCH, muted, 20)); box.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        val clear = iconButton(Glyph.PLUS, "Xóa tìm kiếm", muted, Color.WHITE) { input.setText("") }.apply { rotation = 45f; visibility = if (value.isBlank()) View.INVISIBLE else View.VISIBLE }
        box.addView(clear); watch(input) { clear.visibility = if (it.isBlank()) View.INVISIBLE else View.VISIBLE }
        return box to input
    }
    private fun eventRow(parent: LinearLayout, event: CallEvent, details: Boolean = false) {
        val tint = when (event.action) { Action.BLOCK -> red; Action.SILENCE -> amber; Action.ALLOW -> green }
        val bg = when (event.action) { Action.BLOCK -> Color.rgb(253, 236, 237); Action.SILENCE -> Color.rgb(251, 242, 223); Action.ALLOW -> pale }
        val entry = row().apply { setPadding(0, dp(10), 0, dp(10)); minimumHeight = dp(68); setOnClickListener { eventActions(event) }; isFocusable = true }
        entry.addView(iconTile(if (event.simulated) Glyph.FLASK else if (event.action == Action.BLOCK) Glyph.BLOCK else Glyph.PHONE, tint, bg))
        val summary = column().apply {
            addView(oneLine(event.number, 16f, ink, true)); gap(3)
            addView(oneLine(event.brand ?: when {
                event.reason.contains("danh sách cho phép") -> "Luôn cho phép"
                event.reason.contains("danh bạ") -> "Số trong danh bạ"
                event.reason.contains("tạm dừng", ignoreCase = true) -> "Bảo vệ tạm dừng"
                event.simulated -> "Thử quy tắc"
                else -> "Cuộc gọi đến"
            }, 12f))
        }
        entry.addView(summary, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(12); marginEnd = dp(8) })
        val right = column().apply {
            gravity = Gravity.END
            addView(badge(actionName(event.action), tint, bg), LinearLayout.LayoutParams(-2, -2)); gap(5)
            addView(label(SimpleDateFormat(if (details) "HH:mm · dd/MM" else "HH:mm", Locale("vi", "VN")).format(Date(event.timestamp)), 10f, muted), LinearLayout.LayoutParams(-2, -2))
            if (details) { gap(3); addView(label(if (event.simulated) "Mô phỏng" else "Hệ thống", 10f, muted), LinearLayout.LayoutParams(-2, -2)) }
        }
        entry.addView(right); parent.addView(entry)
    }
    private fun button(text: String, primary: Boolean = true, action: () -> Unit) = Button(this).apply {
        this.text = text; isAllCaps = false; textSize = 14f; minHeight = dp(48); minimumHeight = dp(48)
        setTextColor(if (primary) Color.WHITE else green); background = shape(if (primary) green else pale, 12)
        setPadding(dp(12), dp(6), dp(12), dp(6)); setOnClickListener { action() }
    }
    private fun LinearLayout.action(text: String, primary: Boolean = true, block: () -> Unit) { addView(button(text, primary, block), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) }) }
    private fun field(hint: String, value: String = "", multiline: Boolean = false) = EditText(this).apply {
        this.hint = hint; setText(value); textSize = 15f; setTextColor(ink); setHintTextColor(muted)
        background = shape(Color.rgb(247, 249, 246), 12, true); setPadding(dp(14), dp(12), dp(14), dp(12))
        inputType = InputType.TYPE_CLASS_TEXT or if (multiline) InputType.TYPE_TEXT_FLAG_MULTI_LINE else InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        if (multiline) { minLines = 3; maxLines = 8; gravity = Gravity.TOP } else setSingleLine(true)
    }
    private fun watch(input: EditText, action: (String) -> Unit) {
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { action(s.toString()) }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }
    private fun delayedSearch(action: () -> Unit) {
        pendingSearch?.let(mainHandler::removeCallbacks)
        pendingSearch = Runnable { if (!isDestroyed) action() }
        mainHandler.postDelayed(pendingSearch!!, 220)
    }
    private fun pageControls(parent: LinearLayout, index: Int, total: Int, size: Int, change: (Int) -> Unit) {
        val pages = ((total + size - 1) / size).coerceAtLeast(1)
        parent.addView(label("Trang ${index + 1}/$pages · $total mục", 12f, muted))
        if (pages > 1) {
            val controls = row()
            controls.addView(button("← Trước", false) { change(index - 1) }.apply { isEnabled = index > 0; alpha = if (isEnabled) 1f else 0.4f }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(8) })
            controls.addView(button("Sau →", false) { change(index + 1) }.apply { isEnabled = index + 1 < pages; alpha = if (isEnabled) 1f else 0.4f }, LinearLayout.LayoutParams(0, -2, 1f))
            parent.addView(controls, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        }
    }
    private fun scrollToTop() { content.post { (content.parent as ScrollView).scrollTo(0, 0) } }
    private fun heading(title: String, subtitle: String) {
        val header = row()
        header.addView(iconTile(when (page) { 1 -> Glyph.BRAND; 2 -> Glyph.CLOCK; 3 -> Glyph.SETTINGS; else -> Glyph.SHIELD }))
        val text = column().apply { addView(label(title, 24f, ink, true)); gap(2); addView(oneLine(subtitle, 12f)) }
        header.addView(text, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(12); marginEnd = dp(8) })
        if (!importing && page == 1) header.addView(iconButton(Glyph.PLUS, "Thêm thương hiệu", Color.WHITE, green) { brandEditor() })
        if (!importing && page == 0) header.addView(iconButton(Glyph.SETTINGS, "Mở cài đặt", muted, Color.WHITE) { page = 3; render(); scrollToTop() })
        content.addView(header); content.gap(20)
    }
    private fun toast(message: String) { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
    private fun safe(action: () -> Unit) { try { action() } catch (e: Exception) { toast(e.message ?: "Không thể thực hiện thao tác.") } }
    private fun roleManager() = getSystemService(RoleManager::class.java)
    private fun hasRole() = roleManager()?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
    private fun requestRole() {
        val manager = roleManager()
        if (manager == null || !manager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            toast("Thiết bị không hỗ trợ vai trò lọc cuộc gọi. Bạn vẫn có thể dùng mô phỏng."); return
        }
        if (hasRole()) { toast("QuietCall đã được chọn làm ứng dụng lọc cuộc gọi."); return }
        @Suppress("DEPRECATION")
        startActivityForResult(manager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING), 100)
    }
    private fun render() {
        pendingSearch?.let(mainHandler::removeCallbacks)
        content.removeAllViews(); nav.removeAllViews()
        listOf("Tổng quan", "Thương hiệu", "Lịch sử", "Cài đặt").forEachIndexed { index, title ->
            val selected = page == index
            val item = column().apply {
                gravity = Gravity.CENTER; minimumHeight = dp(62); contentDescription = title; isSelected = selected; isFocusable = true
                val tile = iconTile(listOf(Glyph.HOME, Glyph.BRAND, Glyph.CLOCK, Glyph.SETTINGS)[index], if (selected) green else muted, if (selected) pale else Color.WHITE, 34)
                addView(tile); gap(3)
                addView(label(title, 11f, if (selected) green else muted, selected).apply {
                    gravity = Gravity.CENTER
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                }, LinearLayout.LayoutParams(-1, -2))
                setOnClickListener {
                    if (importing) { toast("Đợi thao tác dữ liệu hoàn tất."); return@setOnClickListener }
                    page = index
                    render()
                    // The new tab must finish layout before its scroll position can be reset.
                    scrollToTop()
                }
            }
            nav.addView(item, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(2); marginEnd = dp(2) })
        }
        if (importing) {
            heading(dataWorkTitle, "Đang xử lý trên thiết bị. Quy tắc cuộc gọi vẫn được hệ thống sử dụng.")
            card().apply { addView(ProgressBar(this@MainActivity)); gap(12); addView(label("Vui lòng đợi thao tác hoàn tất trước khi chỉnh sửa danh mục.", 14f, muted)) }
            return
        }
        when (page) { 0 -> dashboard(); 1 -> brandsPage(); 2 -> historyPage(); else -> settingsPage() }
    }
    private fun dashboard() {
        val snapshot = repository.snapshot()
        val prefs = snapshot.preferences
        val granted = hasRole()
        val ready = granted && prefs.protection
        val blocked = snapshot.brands.filter { it.blocked && it.numbers.isNotEmpty() }
        val status = when { !granted -> "Chưa cấp quyền"; !prefs.protection -> "Đang tạm dừng"; blocked.isEmpty() -> "Chưa bật nhóm chặn"; else -> "Đang bảo vệ" }
        heading("QuietCall", "Cuộc gọi quan trọng, không gián đoạn.")
        card(color = green).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(if (ready) Color.rgb(19, 76, 64) else ink, if (ready) Color.rgb(17, 138, 108) else Color.rgb(47, 82, 76))).apply { cornerRadius = dp(24).toFloat() }
            val body = row()
            val message = column().apply {
                addView(badge(status, Color.WHITE, Color.argb(32, 255, 255, 255)), LinearLayout.LayoutParams(-2, -2)); gap(12)
                addView(label(when { !granted -> "Kích hoạt\nbộ lọc của bạn"; !prefs.protection -> "Một chút\ntạm nghỉ."; blocked.isEmpty() -> "Chọn nhóm\nmuốn chặn."; else -> "An tâm\nnghe máy." }, 23f, Color.WHITE, true)); gap(7)
                addView(label(if (ready && blocked.isNotEmpty()) "${blocked.size} nhóm · ${if (prefs.silence) "Tắt chuông" else "Từ chối"}" else if (!prefs.protection) "Quy tắc vẫn được giữ" else "Thiết lập một lần", 12f, Color.rgb(214, 240, 226)))
            }
            body.addView(message, LinearLayout.LayoutParams(0, -2, 1f))
            body.addView(ShieldScene(this@MainActivity, ready && blocked.isNotEmpty()), LinearLayout.LayoutParams(dp(116), dp(136)))
            addView(body); gap(12)
            val actions = row()
            actions.addView(button(if (!granted) "Kích hoạt bảo vệ" else if (!prefs.protection) "Tiếp tục bảo vệ" else "Quản lý nhóm", false) {
                if (!granted) requestRole() else if (!prefs.protection) { repository.setSetting("protection", true); render() } else { page = 1; render(); scrollToTop() }
            }, LinearLayout.LayoutParams(0, -2, 1f))
            if (ready) actions.addView(iconButton(Glyph.PAUSE, "Tạm dừng bảo vệ", Color.WHITE, Color.argb(30, 255, 255, 255)) { repository.setSetting("protection", false); render() }, LinearLayout.LayoutParams(dp(48), dp(48)).apply { marginStart = dp(10) })
            addView(actions)
        }
        val stats = row()
        listOf("${blocked.sumOf { it.numbers.size }}" to "Số trong nhóm bật", "${repository.processedRealCount()}" to "Chặn / tắt chuông").forEachIndexed { index, (value, caption) ->
            val item = column().apply {
                background = shape(Color.WHITE); setPadding(dp(14), dp(14), dp(14), dp(14))
                val top = row(); top.addView(icon(if (index == 0) Glyph.BRAND else Glyph.BLOCK, if (index == 0) violet else green, 21)); top.addView(label(value, 25f, ink, true), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(10) }); addView(top)
                gap(7); addView(label(caption, 11f, muted))
            }
            stats.addView(item, LinearLayout.LayoutParams(0, -1, 1f).apply { if (index == 0) marginEnd = dp(10) })
        }
        content.addView(stats); content.gap(12)
        val quickActions = row()
        listOf(Triple(Glyph.PLUS, "Thêm nhóm", "Tên và các số"), Triple(Glyph.FLASK, "Thử quy tắc", "Mô phỏng một số")).forEachIndexed { index, (glyph, title, subtitle) ->
            val tile = column().apply {
                background = shape(if (index == 0) pale else Color.rgb(237, 234, 248), 18); setPadding(dp(14), dp(14), dp(14), dp(14)); isFocusable = true
                addView(icon(glyph, if (index == 0) green else violet)); gap(8); addView(label(title, 14f, ink, true)); addView(oneLine(subtitle, 11f))
                setOnClickListener { if (index == 0) brandEditor() else simulator() }
            }
            quickActions.addView(tile, LinearLayout.LayoutParams(0, -1, 1f).apply { if (index == 0) marginEnd = dp(10) })
        }
        content.addView(quickActions); content.gap(16)
        section("Gần đây", "Xem tất cả") { page = 2; historyFilter = 1; historyPageIndex = 0; render(); scrollToTop() }
        val recent = repository.events(filter = 1, limit = 3)
        if (recent.isNotEmpty()) card().apply {
            recent.forEach { eventRow(this, it) }
        } else card().apply {
            gravity = Gravity.CENTER; addView(iconTile(Glyph.PHONE, green, pale, 54)); gap(12)
            addView(label("Chưa có cuộc gọi", 16f, ink, true)); gap(4); addView(label("Các lượt xử lý sẽ xuất hiện tại đây.", 12f, muted))
            if (snapshot.brands.isEmpty()) action("Dùng danh mục demo", false) { safe { repository.seedDemo(); page = 1; render(); scrollToTop() } }
        }
        val privacy = row().apply { gravity = Gravity.CENTER; addView(icon(Glyph.LOCK, muted, 14)); addView(label("  Dữ liệu được giữ trên thiết bị", 11f, muted)) }
        content.addView(privacy)
    }
    private fun brandsPage() {
        heading("Thương hiệu", "Một nhóm số, một lần chặn.")
        val (searchView, input) = searchBox("Tìm thương hiệu…", search)
        content.addView(searchView); content.gap(12)
        val filters = row(); content.addView(filters); content.gap(12)
        if (!hasRole() || !repository.preferences().protection) content.addView(label("Quy tắc được lưu nhưng bộ lọc ${if (!hasRole()) "chưa có quyền hoạt động" else "đang tạm dừng"}. Xem Tổng quan để kích hoạt.", 13f, red)).also { content.gap(12) }
        val list = column(); content.addView(list)
        var all = repository.brands()
        val allowedNumbers = repository.allowed()
        fun fill() {
            list.removeAllViews()
            filters.removeAllViews()
            listOf(false to "Tất cả", true to "Đã bật chặn").forEach { (blockedOnly, caption) ->
                filters.addView(badge(caption, if (onlyBlocked == blockedOnly) Color.WHITE else muted, if (onlyBlocked == blockedOnly) green else Color.WHITE).apply {
                    minHeight = dp(44); setPadding(dp(16), dp(9), dp(16), dp(9)); isFocusable = true; isSelected = onlyBlocked == blockedOnly
                    setOnClickListener { onlyBlocked = blockedOnly; brandPageIndex = 0; fill() }
                }, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = dp(8) })
            }
            val selected = all.filter { PhoneNumbers.key(it.name).contains(PhoneNumbers.key(search)) && (!onlyBlocked || it.blocked) }
            brandPageIndex = brandPageIndex.coerceIn(0, ((selected.size - 1).coerceAtLeast(0) / 25))
            if (selected.isEmpty()) card(list).apply {
                addView(iconTile(Glyph.BRAND, violet, Color.rgb(237, 234, 248), 52)); gap(12)
                addView(label(if (all.isEmpty()) "Danh mục của bạn đang trống" else "Chưa tìm thấy thương hiệu", 18f, ink, true)); gap(8)
                addView(label(if (all.isEmpty()) "Thêm nhóm số hoặc nhập danh mục từ Cài đặt." else "Thử đổi tên tìm kiếm hoặc bộ lọc.", 13f, muted))
                if (all.isEmpty()) action("Nạp dữ liệu demo", false) { safe { repository.seedDemo(); render() } }
                else if (search.trim().length >= 2) action("Tạo thương hiệu “${search.trim()}”", false) { brandEditor(initialName = search.trim()) }
            }
            selected.drop(brandPageIndex * 25).take(25).forEach { brand ->
                card(list).apply {
                    val top = row()
                    val colors = listOf(green to pale, violet to Color.rgb(237, 234, 248), Color.rgb(48, 110, 163) to Color.rgb(228, 239, 250), amber to Color.rgb(251, 242, 223))
                    val (accent, tileColor) = colors[((brand.id - 1) % colors.size).toInt().coerceAtLeast(0)]
                    val initial = brand.name.trim().let { it.substring(0, it.offsetByCodePoints(0, 1)) }.uppercase(Locale.ROOT)
                    val avatar = label(initial, 20f, accent, true).apply { gravity = Gravity.CENTER; background = shape(tileColor, 14) }
                    top.addView(avatar, LinearLayout.LayoutParams(dp(44), dp(44)))
                    val title = column().apply { addView(oneLine(brand.name, 16f, ink, true)); gap(3); addView(label("${brand.numbers.size} số điện thoại", 12f, muted)) }
                    top.addView(title, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(12); marginEnd = dp(8) })
                    top.addView(Switch(this@MainActivity).apply {
                        contentDescription = "Chặn ${brand.name}"; isChecked = brand.blocked
                        setOnCheckedChangeListener { control, enabled ->
                            if (enabled && brand.numbers.isEmpty()) {
                                toast("Thêm ít nhất một số trước khi bật chặn.")
                                control.setOnCheckedChangeListener(null); control.isChecked = false
                                brandEditor(brand)
                                control.post { if (page == 1 && !isDestroyed) fill() }
                            } else {
                                repository.setBlocked(brand.id, enabled)
                                all = all.map { if (it.id == brand.id) it.copy(blocked = enabled) else it }
                                control.post { if (page == 1 && !isDestroyed) fill() }
                            }
                        }
                    })
                    addView(top); gap(12)
                    val exceptions = brand.numbers.count { it in allowedNumbers }
                    val tags = row(); tags.addView(badge(if (brand.blocked) "Chặn bật" else "Chặn tắt", if (brand.blocked) red else muted, if (brand.blocked) Color.rgb(253, 236, 237) else canvasColor))
                    if (exceptions > 0) tags.addView(badge("$exceptions ngoại lệ", green, pale), LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(7) })
                    addView(tags); gap(10)
                    val bottom = row()
                    bottom.addView(oneLine(if (brand.numbers.isEmpty()) "Chưa có số" else brand.numbers.take(2).joinToString(" · ") + if (brand.numbers.size > 2) " +${brand.numbers.size - 2}" else "", 12f), LinearLayout.LayoutParams(0, -2, 1f))
                    bottom.addView(icon(Glyph.CHEVRON, muted, 16)); addView(bottom)
                    contentDescription = "Chi tiết ${brand.name}. ${brand.numbers.size} số."
                    isFocusable = true; setOnClickListener { brandEditor(all.first { it.id == brand.id }) }
                }
            }
            if (selected.isNotEmpty()) pageControls(list, brandPageIndex, selected.size, 25) { brandPageIndex = it; fill(); scrollToTop() }
        }
        watch(input) { search = it; brandPageIndex = 0; delayedSearch { fill() } }
        fill()
    }
    private fun brandEditor(brand: Brand? = null, initialName: String = "", initialNumbers: List<String> = emptyList()) {
        val form = column().apply { setPadding(dp(22), dp(12), dp(22), dp(8)) }
        if (brand != null) {
            val meta = row(); meta.addView(badge("${brand.numbers.size} số", green, pale))
            if (brand.updatedAt > 0) meta.addView(label("  Cập nhật " + SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN")).format(Date(brand.updatedAt)), 11f, muted))
            form.addView(meta); form.gap(12)
        }
        form.addView(label("Tên thương hiệu", 12f, muted)); form.gap(6)
        val name = field("Ví dụ: Dịch vụ quảng cáo A", brand?.name ?: initialName)
        name.filters = arrayOf(InputFilter.LengthFilter(80))
        form.addView(name); form.gap(12)
        form.addView(label("Các số · mỗi dòng hoặc phân cách bằng , ;", 12f, muted)); form.gap(6)
        val numbers = field("0901234567\n+84 901 234 568", (brand?.numbers ?: initialNumbers).joinToString("\n"), true)
        numbers.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        numbers.filters = arrayOf(InputFilter.LengthFilter(30000))
        form.addView(numbers); form.gap(6)
        val numbersPreview = label("", 12f, muted)
        fun parseNumbers() = numbers.text.toString().split(Regex("[\\n,;]+")).map(String::trim).filter(String::isNotBlank)
        fun updateNumberPreview() {
            val parsed = parseNumbers(); val valid = parsed.filter(PhoneNumbers::valid).map(PhoneNumbers::normalize)
            numbersPreview.text = getString(R.string.numbers_preview, valid.distinct().size, valid.size - valid.distinct().size, parsed.size - valid.size)
        }
        form.addView(numbersPreview); watch(numbers) { updateNumberPreview() }; updateNumberPreview(); form.gap(12)
        val note = field("Nguồn / ghi chú (tùy chọn)", brand?.note.orEmpty())
        note.filters = arrayOf(InputFilter.LengthFilter(500))
        form.addView(note); form.gap(10)
        val blocked = Switch(this).apply { setText(R.string.block_this_brand); isChecked = brand?.blocked ?: true }
        form.addView(blocked)
        form.addView(label("Số Việt Nam: +84 / 0084 → 0. Mỗi số thuộc một nhóm.", 12f, muted))
        val errorText = label("", 13f, red).apply { visibility = View.GONE }
        form.gap(8); form.addView(errorText)
        val scroll = ScrollView(this).apply { addView(form) }
        val builder = AlertDialog.Builder(this).setTitle(if (brand == null) "Thêm thương hiệu" else "Chỉnh sửa thương hiệu").setView(scroll).setNegativeButton("Hủy", null).setPositiveButton("Lưu", null)
        if (brand != null) builder.setNeutralButton("Xóa") { _, _ -> confirm("Xóa ${brand.name}?", "Toàn bộ số và quy tắc của thương hiệu sẽ bị xóa.") { repository.deleteBrand(brand.id); render() } }
        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val values = parseNumbers()
                try {
                    repository.saveBrand(brand?.id, name.text.toString(), note.text.toString(), values, blocked.isChecked)
                    dialog.dismiss(); render(); toast(if (blocked.isChecked && !hasRole()) "Đã lưu quy tắc. Kích hoạt lọc cuộc gọi ở Tổng quan để áp dụng." else "Đã lưu thương hiệu.")
                } catch (e: Exception) {
                    errorText.text = e.message ?: "Không thể lưu."; errorText.visibility = View.VISIBLE
                    if (name.text.toString().trim().length < 2 || e.message?.contains("Tên thương hiệu") == true) name.error = e.message
                    else numbers.error = e.message
                    scroll.post { scroll.smoothScrollTo(0, errorText.bottom) }
                }
            }
        }
        dialog.show()
    }
    private fun simulator(initialNumber: String = "") {
        val form = column().apply { setPadding(dp(22), dp(12), dp(22), dp(12)) }
        form.addView(label("Cùng bộ quyết định với dịch vụ Android. Trạng thái quyền hệ thống không ảnh hưởng mô phỏng.", 13f, muted)); form.gap(12)
        val number = field("Số gọi đến", initialNumber).apply { inputType = InputType.TYPE_CLASS_PHONE }
        form.addView(number); form.gap(8)
        val contact = CheckBox(this).apply { setText(R.string.simulate_contact); textSize = 13f }
        form.addView(contact)
        val result = label("Kết quả sẽ hiển thị tại đây.", 15f, muted)
        form.gap(12); form.addView(result)
        form.action("Kiểm tra quy tắc") {
            safe {
                require(PhoneNumbers.valid(number.text.toString())) { "Nhập số hợp lệ từ 8–15 chữ số." }
                val decision = ScreeningEngine.evaluate(number.text.toString(), repository.snapshotForNumber(number.text.toString()), contact.isChecked)
                repository.record(PhoneNumbers.normalize(number.text.toString()), decision, true)
                result.text = getString(R.string.simulation_result, actionName(decision.action), decision.reason)
                result.setTextColor(if (decision.action == Action.BLOCK) red else green)
            }
        }
        AlertDialog.Builder(this).setTitle("Mô phỏng cuộc gọi đến").setView(form).setPositiveButton("Đóng") { _, _ -> render() }.show()
    }
    private fun actionName(action: Action) = when (action) { Action.BLOCK -> "Đã chặn"; Action.SILENCE -> "Tắt chuông"; Action.ALLOW -> "Cho phép" }
    private fun historyPage() {
        heading("Lịch sử", "Cuộc gọi và kết quả xử lý.")
        val (searchView, input) = searchBox("Tìm số hoặc thương hiệu…", historyQuery)
        content.addView(searchView); content.gap(12)
        val filters = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("Tất cả", "Cuộc gọi hệ thống", "Mô phỏng", "Đã chặn / tắt chuông"))
            setSelection(historyFilter)
        }
        filters.background = shape(Color.WHITE, 14)
        val tools = row(); tools.addView(filters, LinearLayout.LayoutParams(0, dp(48), 1f))
        tools.addView(iconButton(Glyph.FLASK, "Thử quy tắc", violet, Color.rgb(237, 234, 248)) { simulator() }, LinearLayout.LayoutParams(dp(48), dp(48)).apply { marginStart = dp(10) })
        content.addView(tools); content.gap(14)
        val list = column(); content.addView(list)
        fun fill() {
            list.removeAllViews()
            val count = repository.eventCount(historyFilter, historyQuery)
            historyPageIndex = historyPageIndex.coerceIn(0, (count - 1).coerceAtLeast(0) / 25)
            val events = repository.events(historyFilter, 25, historyPageIndex * 25, historyQuery)
            if (events.isEmpty()) card(list).apply {
                gravity = Gravity.CENTER; addView(iconTile(Glyph.CLOCK, violet, Color.rgb(237, 234, 248), 56)); gap(14)
                addView(label(if (historyQuery.isBlank()) "Chưa có lượt xử lý" else "Không có kết quả phù hợp", 18f, ink, true)); gap(8)
                addView(label(if (historyQuery.isNotBlank()) "Thử đổi số, tên thương hiệu hoặc bộ lọc để tìm lại." else "Kích hoạt lọc để ghi nhận cuộc gọi hệ thống, hoặc dùng mô phỏng để trình diễn bộ chặn.", 14f, muted))
                action("Thử mô phỏng", false) { simulator() }
            }
            events.forEach { event -> card(list).apply {
                setPadding(dp(14), dp(2), dp(14), dp(2)); eventRow(this, event, true)
            } }
            if (count > 0) pageControls(list, historyPageIndex, count, 25) { historyPageIndex = it; fill(); scrollToTop() }
        }
        watch(input) { historyQuery = it; historyPageIndex = 0; delayedSearch { fill() } }
        filters.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (historyFilter != position) { historyFilter = position; historyPageIndex = 0; fill() }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        fill()
    }
    private fun eventActions(event: CallEvent) {
        if (!PhoneNumbers.valid(event.number)) { toast("Không có số hợp lệ để quản lý."); return }
        val allowed = repository.snapshotForNumber(event.number).allowed.isNotEmpty()
        val title = column().apply {
            setPadding(dp(24), dp(22), dp(24), dp(12)); addView(label(event.number, 22f, ink, true)); gap(6)
            addView(label(event.reason, 13f, muted)); gap(10)
            addView(badge("${actionName(event.action)} · ${if (event.simulated) "Mô phỏng" else "Hệ thống"}", if (event.action == Action.BLOCK) red else green, if (event.action == Action.BLOCK) Color.rgb(253, 236, 237) else pale))
        }
        AlertDialog.Builder(this).setCustomTitle(title).setItems(arrayOf(if (allowed) "Bỏ khỏi danh sách cho phép" else "Luôn cho phép số này", "Gán số vào thương hiệu", "Mô phỏng lại", "Sao chép số")) { _, which ->
            when (which) {
                0 -> safe {
                    if (allowed) repository.removeAllowed(PhoneNumbers.normalize(event.number)) else repository.addAllowed(event.number)
                    toast(if (allowed) "Đã bỏ ngoại lệ cho phép." else "Đã cho phép số này."); render()
                }
                1 -> {
                    val brands = repository.brands()
                    if (brands.isEmpty()) brandEditor(initialNumbers = listOf(event.number))
                    else AlertDialog.Builder(this).setTitle("Chọn thương hiệu").setItems((brands.map { it.name } + "+ Tạo thương hiệu mới").toTypedArray()) { _, index ->
                        if (index == brands.size) brandEditor(initialNumbers = listOf(event.number))
                        else safe {
                            val brand = brands[index]
                            repository.saveBrand(brand.id, brand.name, brand.note, (brand.numbers + event.number).distinct(), brand.blocked)
                            toast("Đã thêm số vào ${brand.name}."); render()
                        }
                    }.show()
                }
                2 -> simulator(event.number)
                3 -> { getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Số điện thoại", event.number)); toast("Đã sao chép số.") }
            }
        }.show()
    }
    private fun LinearLayout.toggle(title: String, description: String, enabled: Boolean, change: (Boolean) -> Unit) {
        val entry = row().apply { setPadding(0, dp(10), 0, dp(10)); minimumHeight = dp(72) }
        entry.addView(iconTile(when { title.contains("chuông") -> Glyph.BELL; title.contains("danh bạ") -> Glyph.USER; else -> Glyph.SHIELD }, green, pale, 38))
        val summary = column().apply { addView(label(title, 14f, ink, true)); gap(3); addView(oneLine(description, 11f)) }
        entry.addView(summary, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(10); marginEnd = dp(6) })
        val control = Switch(this@MainActivity).apply { contentDescription = title; isChecked = enabled; setOnCheckedChangeListener { _, checked -> change(checked) } }
        entry.addView(control); addView(entry)
    }
    private fun settingsPage() {
        val prefs = repository.preferences()
        heading("Cài đặt", "Theo cách bạn muốn.")
        card().apply {
            val state = row(); state.addView(iconTile(Glyph.SHIELD))
            val summary = column().apply { addView(label("Bộ lọc cuộc gọi", 16f, ink, true)); gap(3); addView(label("Android 10 trở lên", 12f, muted)) }
            state.addView(summary, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(12) }); state.addView(badge(if (hasRole()) "Đã cấp" else "Chưa cấp", if (hasRole()) green else amber, if (hasRole()) pale else Color.rgb(251, 242, 223)))
            addView(state); gap(8)
            if (!hasRole()) action("Kích hoạt lọc cuộc gọi") { requestRole() }
            settingEntry(this, Glyph.USER, "Quyền danh bạ", if (Contacts.canRead(this@MainActivity)) "Đã cấp · bảo vệ số liên hệ" else "Tùy chọn · chưa cấp") {
                if (Contacts.canRead(this@MainActivity)) appSettings() else requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 101)
            }
            settingEntry(this, Glyph.SETTINGS, "Quyền ứng dụng", "Quản lý trong cài đặt Android") { appSettings() }
        }
        section("Cách bảo vệ")
        card().apply {
            toggle("Bật bảo vệ", "Tắt để tạm cho phép cuộc gọi", prefs.protection) { repository.setSetting("protection", it) }
            toggle("Chỉ tắt chuông", "Cuộc gọi vẫn có thể hiển thị", prefs.silence) { repository.setSetting("silence", it) }
            toggle("Ưu tiên danh bạ", "Cho phép số liên hệ đã lưu", prefs.protectContacts) { repository.setSetting("protectContacts", it) }
        }
        section("Luôn cho phép", "Thêm số") { allowedEditor() }
        card().apply {
            val count = repository.allowed().size
            settingEntry(this, Glyph.CHECK, "Danh sách ngoại lệ", if (count == 0) "Chưa có số · ưu tiên hơn quy tắc chặn" else "$count số · luôn được cho phép") { allowedManager() }
        }
        section("Danh mục & dữ liệu")
        card().apply {
            settingEntry(this, Glyph.IMPORT, "Nhập danh mục", "JSON · xem trước rồi gộp") { chooseImport() }
            settingEntry(this, Glyph.EXPORT, "Xuất danh mục", "JSON · gồm danh sách ngoại lệ") { chooseExport() }
            settingEntry(this, Glyph.FLASK, "Danh mục demo", "Dữ liệu giả lập để dùng thử", violet) { safe { repository.seedDemo(); toast("Đã nạp dữ liệu giả lập, chưa bật chặn nhóm mới."); render() } }
        }
        section("Dọn lịch sử")
        card().apply {
            settingEntry(this, Glyph.FLASK, "Xóa lượt mô phỏng", "Giữ lịch sử cuộc gọi hệ thống", violet) { confirm("Xóa lượt mô phỏng?", "Giữ nguyên lịch sử cuộc gọi hệ thống và mọi quy tắc.") { repository.clearSimulations(); render() } }
            settingEntry(this, Glyph.TRASH, "Xóa toàn bộ lịch sử", "Giữ nguyên danh mục và quy tắc", red) { confirm("Xóa toàn bộ lịch sử?", "Xóa cuộc gọi hệ thống và mô phỏng. Không thay đổi quy tắc chặn.") { repository.clearHistory(); render() } }
        }
        card(color = pale).apply {
            settingEntry(this, Glyph.LOCK, "Riêng tư trên thiết bị", "Không Internet · không tài khoản") { aboutDialog() }
        }
        content.addView(label("QuietCall 1.0.0", 11f, muted).apply { gravity = Gravity.CENTER })
    }
    private fun appSettings() { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:$packageName"))) }
    private fun aboutDialog() {
        AlertDialog.Builder(this).setTitle("Quyền riêng tư & phạm vi")
            .setMessage("Dữ liệu nằm trên thiết bị. Không có quyền Internet, không gửi số lên máy chủ, không đọc nhật ký cuộc gọi hệ thống; sao lưu tự động đã tắt.\n\nQuietCall chỉ đối chiếu số trong danh mục. Hỗ trợ cuộc gọi điện thoại Android 10+, không lọc Zalo/Messenger.\n\nChế độ tắt chuông vẫn có thể hiện giao diện cuộc gọi. Khi chưa có quyền danh bạ, Android thường chỉ chuyển số ngoài danh bạ đến bộ lọc.\n\nLưu riêng 500 cuộc gọi hệ thống và 100 lượt mô phỏng.")
            .setPositiveButton("Đã hiểu", null).show()
    }
    private fun allowedEditor() {
        val input = field("Số điện thoại").apply { inputType = InputType.TYPE_CLASS_PHONE }
        val container = column().apply { setPadding(dp(22), dp(12), dp(22), dp(12)); addView(input) }
        val dialog = AlertDialog.Builder(this).setTitle("Luôn cho phép số").setView(container).setNegativeButton("Hủy", null).setPositiveButton("Lưu", null).create()
        dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            try { repository.addAllowed(input.text.toString()); dialog.dismiss(); render() } catch (e: Exception) { input.error = e.message }
        } }
        dialog.show()
    }
    private fun allowedManager() {
        val form = column().apply { setPadding(dp(20), dp(12), dp(20), dp(12)) }
        val input = field("Tìm số luôn cho phép…").apply { inputType = InputType.TYPE_CLASS_PHONE }
        form.addView(input); form.gap(10)
        val list = column(); form.addView(list)
        val scroll = ScrollView(this).apply { addView(form) }
        var query = ""; var index = 0
        var numbers = repository.allowed().sorted()
        fun fill() {
            list.removeAllViews()
            val selected = numbers.filter { it.contains(query.filter(Char::isDigit)) }
            index = index.coerceIn(0, (selected.size - 1).coerceAtLeast(0) / 20)
            selected.drop(index * 20).take(20).forEach { number ->
                val entry = row()
                entry.addView(label(number, 15f), LinearLayout.LayoutParams(0, -2, 1f))
                entry.addView(button("Bỏ cho phép", false) { confirm("Bỏ cho phép $number?", "Số này sẽ được đối chiếu với quy tắc thương hiệu.") {
                    repository.removeAllowed(number); numbers = repository.allowed().sorted(); fill(); render()
                } })
                list.addView(entry, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
            }
            if (selected.isEmpty()) list.addView(label("Không tìm thấy số phù hợp.", 14f, muted))
            else pageControls(list, index, selected.size, 20) { index = it; fill(); scroll.post { scroll.scrollTo(0, 0) } }
        }
        watch(input) { query = it; index = 0; fill() }; fill()
        AlertDialog.Builder(this).setTitle("Số luôn cho phép").setView(scroll).setPositiveButton("Đóng") { _, _ -> render() }.show()
    }
    private fun confirm(title: String, message: String, action: () -> Unit) {
        AlertDialog.Builder(this).setTitle(title).setMessage(message).setNegativeButton("Hủy", null).setPositiveButton("Xác nhận") { _, _ -> safe(action) }.show()
    }
    @Suppress("DEPRECATION")
    private fun chooseImport() { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "*/*"; addCategory(Intent.CATEGORY_OPENABLE); putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/plain", "application/octet-stream")) }, 201) }
    @Suppress("DEPRECATION")
    private fun chooseExport() { startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply { type = "application/json"; addCategory(Intent.CATEGORY_OPENABLE); putExtra(Intent.EXTRA_TITLE, "quietcall-catalog.json") }, 202) }

    private fun <T> dataWork(title: String, work: (QuietRepository) -> T, success: (T) -> Unit) {
        if (importing) return
        importing = true; dataWorkTitle = title; render(); scrollToTop()
        io.execute {
            val outcome = runCatching { QuietRepository(applicationContext).use { work(it) } }
            runOnUiThread {
                importing = false
                if (!isDestroyed && !isFinishing) {
                    render()
                    outcome.fold(success, { error -> AlertDialog.Builder(this).setTitle("Không thể hoàn tất")
                        .setMessage(error.message ?: "Dữ liệu không hợp lệ hoặc không truy cập được tệp.")
                        .setPositiveButton("Đóng", null).show() })
                }
            }
        }
    }
    private fun importPreview(text: String, preview: ImportPreview) {
        if (preview.conflicts.isNotEmpty()) {
            AlertDialog.Builder(this).setTitle("Danh mục có xung đột")
                .setMessage("Chưa thay đổi dữ liệu. Sửa các mục sau trước khi nhập:\n\n" + preview.conflicts.take(8).joinToString("\n") + if (preview.conflicts.size > 8) "\n… và ${preview.conflicts.size - 8} xung đột khác." else "")
                .setPositiveButton("Đóng", null).show()
            return
        }
        val form = column().apply { setPadding(dp(22), dp(12), dp(22), dp(12)) }
        form.addView(label("${preview.brands} thương hiệu trong tệp\n${preview.newBrands} thương hiệu mới\n${preview.newNumbers} số mới\n${preview.duplicates} số trùng được gộp", 15f, ink))
        form.gap(12)
        form.addView(label("Thương hiệu mới chưa bật chặn. Trạng thái chặn và ghi chú của thương hiệu hiện có được giữ nguyên.", 13f, muted))
        if (preview.newBlockedNumbers > 0) { form.gap(8); form.addView(label("${preview.newBlockedNumbers} số mới sẽ vào các nhóm đã bật quy tắc chặn; ngoại lệ vẫn được ưu tiên.", 13f, green)) }
        val restoreAllowed = CheckBox(this).apply { this.text = getString(R.string.restore_allowed, preview.newAllowedNumbers); textSize = 13f }
        if (preview.newAllowedNumbers > 0) {
            form.gap(12); form.addView(restoreAllowed)
            form.addView(label("Chỉ chọn khi muốn khôi phục ngoại lệ từ bản sao lưu bạn tin cậy. Mặc định chỉ nhập danh mục thương hiệu.", 12f, muted))
        }
        AlertDialog.Builder(this).setTitle("Xem trước danh mục").setView(ScrollView(this).apply { addView(form) }).setNegativeButton("Hủy", null)
            .setPositiveButton("Gộp dữ liệu") { _, _ ->
                dataWork("Đang gộp danh mục", { it.importJson(text, restoreAllowed.isChecked) }) { count ->
                    toast("Đã gộp $count thương hiệu. Thương hiệu mới chưa bật chặn.")
                }
            }.show()
    }

    @Deprecated("Platform Activity callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) { if (requestCode == 100) { toast("Chưa cấp vai trò lọc. Bạn vẫn có thể mô phỏng."); render() }; return }
        if (requestCode == 100) { render(); return }
        val uri = data?.data ?: return
        if (requestCode == 201) {
            dataWork("Đang đọc danh mục", { repo ->
                    val bytes = contentResolver.openInputStream(uri)?.use { input ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        var count = input.read(buffer)
                        while (count != -1) {
                            require(output.size() + count <= 1_000_000) { "Tệp quá lớn (tối đa 1 MB)." }
                            output.write(buffer, 0, count); count = input.read(buffer)
                        }
                        output.toByteArray()
                    } ?: error("Không đọc được tệp.")
                    val text = bytes.toString(Charsets.UTF_8)
                    text to repo.previewImport(text)
            }) { (text, preview) -> importPreview(text, preview) }
        } else if (requestCode == 202) dataWork("Đang xuất danh mục", { repo ->
            val json = repo.exportJson()
            contentResolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray(Charsets.UTF_8)) } ?: error("Không ghi được tệp.")
        }) { toast("Đã xuất danh mục JSON, gồm cả danh sách luôn cho phép; không có lịch sử cuộc gọi.") }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) { toast(if (Contacts.canRead(this)) "Đã cấp quyền danh bạ." else "Chưa cấp quyền danh bạ. Có thể cấp lại trong thông tin ứng dụng."); render() }
    }
}
