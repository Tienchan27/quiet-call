package vn.quietcall.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import vn.quietcall.domain.*

data class ImportPreview(val brands: Int, val newBrands: Int, val newNumbers: Int, val newBlockedNumbers: Int,
    val newAllowedNumbers: Int, val duplicates: Int, val conflicts: List<String>)
private data class ImportBrand(val name: String, val note: String, val numbers: List<String>)
private data class ImportCatalog(val brands: List<ImportBrand>, val allowed: Set<String>, val duplicates: Int)

class QuietRepository(context: Context) : SQLiteOpenHelper(context.applicationContext, "quietcall.db", null, 2) {
    init { setWriteAheadLoggingEnabled(true) }
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE brands(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,name_key TEXT NOT NULL UNIQUE,note TEXT NOT NULL DEFAULT '',blocked INTEGER NOT NULL DEFAULT 0,updated_at INTEGER NOT NULL DEFAULT 0)")
        db.execSQL("CREATE TABLE numbers(number TEXT PRIMARY KEY,brand_id INTEGER NOT NULL REFERENCES brands(id) ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX numbers_brand ON numbers(brand_id)")
        db.execSQL("CREATE TABLE allowed(number TEXT PRIMARY KEY)")
        db.execSQL("CREATE TABLE settings(key TEXT PRIMARY KEY,value INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE events(id INTEGER PRIMARY KEY AUTOINCREMENT,number TEXT NOT NULL,action TEXT NOT NULL,reason TEXT NOT NULL,brand TEXT,created_at INTEGER NOT NULL,simulated INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX events_type_id ON events(simulated,id DESC)")
    }
    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE brands ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
            db.execSQL("CREATE INDEX IF NOT EXISTS events_type_id ON events(simulated,id DESC)")
            db.execSQL("DELETE FROM events WHERE simulated=1 AND id NOT IN (SELECT id FROM events WHERE simulated=1 ORDER BY id DESC LIMIT 100)")
        }
    }

    fun brands(): List<Brand> {
        val numbers = mutableMapOf<Long, MutableList<String>>()
        readableDatabase.rawQuery("SELECT brand_id,number FROM numbers ORDER BY number", null).use { c ->
            while (c.moveToNext()) numbers.getOrPut(c.getLong(0)) { mutableListOf() }.add(c.getString(1))
        }
        val result = mutableListOf<Brand>()
        readableDatabase.rawQuery("SELECT id,name,note,blocked,updated_at FROM brands ORDER BY name COLLATE NOCASE", null).use { c ->
            while (c.moveToNext()) result.add(Brand(c.getLong(0), c.getString(1), c.getString(2), c.getInt(3) == 1, numbers[c.getLong(0)].orEmpty(), c.getLong(4)))
        }
        return result
    }
    fun allowed(): Set<String> = buildSet {
        readableDatabase.rawQuery("SELECT number FROM allowed ORDER BY number", null).use { c -> while (c.moveToNext()) add(c.getString(0)) }
    }
    fun preferences(): Preferences {
        val values = mutableMapOf<String, Boolean>()
        readableDatabase.rawQuery("SELECT key,value FROM settings", null).use { c -> while (c.moveToNext()) values[c.getString(0)] = c.getInt(1) == 1 }
        return Preferences(values["protection"] ?: true, values["silence"] ?: false, values["protectContacts"] ?: true)
    }
    fun snapshot() = Snapshot(brands(), allowed(), preferences())

    /** One consistent SQL statement, indexed by the incoming number. Never loads the whole catalog. */
    fun snapshotForNumber(raw: String): Snapshot {
        val number = PhoneNumbers.normalize(raw)
        val sql = """SELECT
            COALESCE((SELECT value FROM settings WHERE key='protection'),1),
            COALESCE((SELECT value FROM settings WHERE key='silence'),0),
            COALESCE((SELECT value FROM settings WHERE key='protectContacts'),1),
            a.number IS NOT NULL,b.id,b.name,b.note,b.blocked,b.updated_at
            FROM (SELECT ? AS caller) q
            LEFT JOIN numbers n ON n.number=q.caller
            LEFT JOIN brands b ON b.id=n.brand_id
            LEFT JOIN allowed a ON a.number=q.caller""".trimIndent()
        return readableDatabase.rawQuery(sql, arrayOf(number)).use { c ->
            check(c.moveToFirst())
            val prefs = Preferences(c.getInt(0) == 1, c.getInt(1) == 1, c.getInt(2) == 1)
            val brand = if (c.isNull(4)) emptyList() else listOf(Brand(c.getLong(4), c.getString(5), c.getString(6), c.getInt(7) == 1, listOf(number), c.getLong(8)))
            Snapshot(brand, if (c.getInt(3) == 1) setOf(number) else emptySet(), prefs)
        }
    }
    fun setSetting(key: String, value: Boolean) {
        writableDatabase.insertWithOnConflict("settings", null, ContentValues().apply { put("key", key); put("value", if (value) 1 else 0) }, SQLiteDatabase.CONFLICT_REPLACE)
    }
    fun saveBrand(id: Long?, name: String, note: String, rawNumbers: List<String>, blocked: Boolean) {
        require(name.trim().length in 2..80) { "Tên thương hiệu cần từ 2 đến 80 ký tự." }
        require(note.trim().length <= 500) { "Ghi chú tối đa 500 ký tự." }
        require(rawNumbers.size <= 1000) { "Tối đa 1.000 số mỗi thương hiệu." }
        require(rawNumbers.all(PhoneNumbers::valid)) { "Mỗi số cần 8–15 chữ số; chỉ dùng số, +, dấu cách, (), dấu chấm hoặc gạch nối." }
        require(!blocked || rawNumbers.isNotEmpty()) { "Cần ít nhất một số điện thoại để bật chặn thương hiệu." }
        val db = writableDatabase
        val numbers = rawNumbers.map(PhoneNumbers::normalize).distinct()
        db.beginTransaction()
        try {
            db.rawQuery("SELECT id FROM brands WHERE name_key=?", arrayOf(PhoneNumbers.key(name))).use { c ->
                require(!c.moveToFirst() || c.getLong(0) == id) { "Tên thương hiệu này đã tồn tại." }
            }
            for (number in numbers) db.rawQuery("SELECT brand_id FROM numbers WHERE number=?", arrayOf(number)).use { c ->
                require(!c.moveToFirst() || c.getLong(0) == id) { "Số $number đã thuộc thương hiệu khác." }
            }
            val values = ContentValues().apply { put("name", name.trim()); put("name_key", PhoneNumbers.key(name)); put("note", note.trim()); put("blocked", if (blocked) 1 else 0); put("updated_at", System.currentTimeMillis()) }
            val brandId = id ?: db.insertOrThrow("brands", null, values)
            if (id != null) {
                require(db.update("brands", values, "id=?", arrayOf(id.toString())) == 1)
                db.delete("numbers", "brand_id=?", arrayOf(id.toString()))
            }
            numbers.forEach { number -> db.insertOrThrow("numbers", null, ContentValues().apply { put("number", number); put("brand_id", brandId) }) }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
    fun setBlocked(id: Long, blocked: Boolean) {
        if (blocked) readableDatabase.rawQuery("SELECT 1 FROM numbers WHERE brand_id=? LIMIT 1", arrayOf(id.toString())).use { require(it.moveToFirst()) { "Thêm ít nhất một số trước khi bật chặn." } }
        writableDatabase.execSQL("UPDATE brands SET blocked=? WHERE id=?", arrayOf(if (blocked) 1 else 0, id))
    }
    fun deleteBrand(id: Long) { writableDatabase.delete("brands", "id=?", arrayOf(id.toString())) }
    fun addAllowed(raw: String) {
        require(PhoneNumbers.valid(raw)) { "Số điện thoại không hợp lệ (8–15 chữ số)." }
        writableDatabase.insertWithOnConflict("allowed", null, ContentValues().apply { put("number", PhoneNumbers.normalize(raw)) }, SQLiteDatabase.CONFLICT_IGNORE)
    }
    fun removeAllowed(number: String) { writableDatabase.delete("allowed", "number=?", arrayOf(number)) }
    fun record(number: String, decision: Decision, simulated: Boolean) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.insertOrThrow("events", null, ContentValues().apply {
                put("number", PhoneNumbers.normalize(number).ifBlank { "Ẩn số" }); put("action", decision.action.name); put("reason", decision.reason)
                put("brand", decision.brand); put("created_at", System.currentTimeMillis()); put("simulated", if (simulated) 1 else 0)
            })
            val kind = if (simulated) 1 else 0
            db.execSQL("DELETE FROM events WHERE simulated=? AND id NOT IN (SELECT id FROM events WHERE simulated=? ORDER BY id DESC LIMIT ?)", arrayOf(kind, kind, if (simulated) 100 else 500))
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
    private fun historyWhere(filter: Int, query: String): Pair<String, Array<String>> {
        val conditions = mutableListOf<String>()
        val args = mutableListOf<String>()
        when (filter) { 1 -> conditions.add("simulated=0"); 2 -> conditions.add("simulated=1"); 3 -> conditions.add("action IN ('BLOCK','SILENCE')") }
        if (query.isNotBlank()) {
            conditions.add("(number LIKE ? OR brand LIKE ? OR reason LIKE ? OR REPLACE(number,'+84','0') LIKE ?)")
            val text = "%${query.trim()}%"
            args.addAll(listOf(text, text, text, "%${if (PhoneNumbers.valid(query)) PhoneNumbers.normalize(query) else query.trim()}%"))
        }
        return (if (conditions.isEmpty()) "" else " WHERE " + conditions.joinToString(" AND ")) to args.toTypedArray()
    }
    fun eventCount(filter: Int = 0, query: String = ""): Int {
        val (where, args) = historyWhere(filter, query)
        return readableDatabase.rawQuery("SELECT COUNT(*) FROM events$where", args).use { it.moveToFirst(); it.getInt(0) }
    }
    fun processedRealCount(): Int = readableDatabase.rawQuery("SELECT COUNT(*) FROM events WHERE simulated=0 AND action IN ('BLOCK','SILENCE')", null).use { it.moveToFirst(); it.getInt(0) }
    fun events(filter: Int = 0, limit: Int = 25, offset: Int = 0, query: String = ""): List<CallEvent> = buildList {
        val (where, args) = historyWhere(filter, query)
        readableDatabase.rawQuery("SELECT id,number,action,reason,brand,created_at,simulated FROM events$where ORDER BY id DESC LIMIT ? OFFSET ?", args + arrayOf(limit.coerceIn(1, 600).toString(), offset.coerceAtLeast(0).toString())).use { c ->
            while (c.moveToNext()) add(CallEvent(c.getLong(0), c.getString(1), Action.valueOf(c.getString(2)), c.getString(3), if (c.isNull(4)) null else c.getString(4), c.getLong(5), c.getInt(6) == 1))
        }
    }
    fun clearHistory() { writableDatabase.delete("events", null, null) }
    fun clearSimulations() { writableDatabase.delete("events", "simulated=1", null) }
    fun seedDemo() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            listOf(
                Triple("Aster Finance · Demo", "Tài chính • Dữ liệu giả lập, không đại diện doanh nghiệp thật", listOf("0900000001", "0900000002", "0900000003")),
                Triple("Nova Homes · Demo", "Bất động sản • Dữ liệu giả lập", listOf("0900000011", "0900000012")),
                Triple("Orbit Ads · Demo", "Quảng cáo • Dữ liệu giả lập", listOf("0900000021", "0900000022"))
            ).forEach { (name, note, numbers) ->
                if (findBrand(name) == null) saveBrand(null, name, note, numbers, false)
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
    fun exportJson(): String = JSONObject().apply {
        put("schemaVersion", 1)
        put("brands", JSONArray().apply { brands().forEach { b -> put(JSONObject().apply { put("name", b.name); put("note", b.note); put("blocked", b.blocked); put("numbers", JSONArray(b.numbers)) }) } })
        put("allowed", JSONArray(allowed().toList()))
    }.toString(2)

    private fun parseImport(text: String): ImportCatalog {
        require(text.length <= 1_000_000) { "Tệp quá lớn (tối đa 1 MB)." }
        val root = JSONObject(text)
        require(root.getInt("schemaVersion") == 1) { "Phiên bản dữ liệu không được hỗ trợ." }
        val data = root.getJSONArray("brands")
        val allowedData = root.optJSONArray("allowed") ?: JSONArray()
        require(data.length() <= 500 && allowedData.length() <= 5000) { "Tối đa 500 thương hiệu và 5.000 số cho phép mỗi lần nhập." }
        val keys = mutableSetOf<String>()
        var duplicates = 0
        val brands = (0 until data.length()).map { i ->
                val item = data.getJSONObject(i)
                val name = item.getString("name").trim()
                require(name.length in 2..80) { "Tên thương hiệu cần từ 2 đến 80 ký tự." }
                require(keys.add(PhoneNumbers.key(name))) { "Thương hiệu $name bị lặp trong tệp." }
                val note = item.optString("note").trim()
                require(note.length <= 500) { "Ghi chú của $name vượt quá 500 ký tự." }
                val numberData = item.getJSONArray("numbers")
                require(numberData.length() <= 1000) { "Tối đa 1.000 số mỗi thương hiệu." }
                val numbers = (0 until numberData.length()).map { numberData.getString(it) }.map { raw ->
                    require(PhoneNumbers.valid(raw)) { "Số không hợp lệ trong $name: $raw" }; PhoneNumbers.normalize(raw)
                }
                duplicates += numbers.size - numbers.distinct().size
                ImportBrand(name, note, numbers.distinct())
        }
        val allowed = (0 until allowedData.length()).map { allowedData.getString(it) }.map { raw ->
            require(PhoneNumbers.valid(raw)) { "Số cho phép không hợp lệ: $raw" }; PhoneNumbers.normalize(raw)
        }.toSet()
        return ImportCatalog(brands, allowed, duplicates)
    }
    fun previewImport(text: String): ImportPreview {
        val catalog = parseImport(text)
        val existing = brands().associateBy { PhoneNumbers.key(it.name) }
        val owners = existing.values.flatMap { b -> b.numbers.map { it to PhoneNumbers.key(b.name) } }.toMap().toMutableMap()
        var newBrands = 0; var newNumbers = 0; var blockedNumbers = 0; var duplicates = catalog.duplicates
        val conflicts = mutableListOf<String>()
        catalog.brands.forEach { b ->
            val key = PhoneNumbers.key(b.name)
            val current = existing[key]
            if (current == null) newBrands++
            if ((current?.numbers.orEmpty() + b.numbers).distinct().size > 1000) conflicts.add("${b.name}: sau khi gộp sẽ vượt 1.000 số.")
            b.numbers.forEach { number ->
                val owner = owners[number]
                when {
                    owner == key -> duplicates++
                    owner != null -> conflicts.add("$number đã thuộc ${existing[owner]?.name ?: owner}, không thể gán cho ${b.name}.")
                    else -> { owners[number] = key; newNumbers++; if (current?.blocked == true) blockedNumbers++ }
                }
            }
        }
        return ImportPreview(catalog.brands.size, newBrands, newNumbers, blockedNumbers, (catalog.allowed - allowed()).size, duplicates, conflicts)
    }
    private fun findBrand(name: String): Brand? = readableDatabase.rawQuery("SELECT id,name,note,blocked,updated_at FROM brands WHERE name_key=?", arrayOf(PhoneNumbers.key(name))).use { c ->
        if (!c.moveToFirst()) null else {
            val id = c.getLong(0)
            val numbers = readableDatabase.rawQuery("SELECT number FROM numbers WHERE brand_id=? ORDER BY number", arrayOf(id.toString())).use { n -> buildList { while (n.moveToNext()) add(n.getString(0)) } }
            Brand(id, c.getString(1), c.getString(2), c.getInt(3) == 1, numbers, c.getLong(4))
        }
    }
    /** Explicitly opt into restoring whitelist entries. Validate again inside the write transaction. */
    fun importJson(text: String, includeAllowed: Boolean = false): Int {
        val catalog = parseImport(text)
        val db = writableDatabase
        db.beginTransaction()
        try {
            catalog.brands.forEach { b ->
                val existing = findBrand(b.name)
                saveBrand(existing?.id, b.name, existing?.note ?: b.note, (existing?.numbers.orEmpty() + b.numbers).distinct(), existing?.blocked ?: false)
            }
            if (includeAllowed) catalog.allowed.forEach(::addAllowed)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        return catalog.brands.size
    }
}
