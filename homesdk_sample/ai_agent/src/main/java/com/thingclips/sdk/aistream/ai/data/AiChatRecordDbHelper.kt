package com.thingclips.sdk.aistream.ai.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

data class ChatMessageRecord(
    val id: Long = 0,
    val devId: String,
    val roleId: String,
    val bizId: String?,
    val sender: Int,        // 0 = user, 1 = ai
    val msgType: String,    // text / voice_to_text / nlg_text / image / nlg_image
    val content: String?,
    val imageUri: String?,
    val ts: Long
)

class AiChatRecordDbHelper(context: Context, uid: String) :
    SQLiteOpenHelper(context.applicationContext, "thing_ai_chat_$uid", null, 1) {

    companion object {
        private const val TAG = "ai_stream_ChatDb"
        private const val T = "chat_messages"
        private const val C_ID = "id"
        private const val C_DEV = "dev_id"
        private const val C_ROLE = "role_id"
        private const val C_BIZ = "biz_id"
        private const val C_SENDER = "sender"
        private const val C_TYPE = "msg_type"
        private const val C_CONTENT = "content"
        private const val C_IMG = "image_uri"
        private const val C_TS = "ts"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("PRAGMA disable_load_extension=ON")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS $T($C_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$C_DEV TEXT,$C_ROLE TEXT,$C_BIZ TEXT,$C_SENDER INTEGER,$C_TYPE TEXT," +
                "$C_CONTENT TEXT,$C_IMG TEXT,$C_TS INTEGER)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_chat ON $T($C_DEV,$C_ROLE,$C_TS)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $T")
        onCreate(db)
    }

    fun insert(r: ChatMessageRecord): Long {
        return try {
            val v = ContentValues().apply {
                put(C_DEV, r.devId)
                put(C_ROLE, r.roleId)
                put(C_BIZ, r.bizId)
                put(C_SENDER, r.sender)
                put(C_TYPE, r.msgType)
                put(C_CONTENT, r.content)
                put(C_IMG, r.imageUri)
                put(C_TS, r.ts)
            }
            writableDatabase.insert(T, null, v)
        } catch (e: Exception) {
            Log.e(TAG, "insert error: ${e.message}")
            -1
        }
    }

    /**
     * Upsert a streaming NLG reply by bizId: the cloud delivers it in append
     * chunks sharing one bizId, so update the existing row's content instead of
     * keeping only the first chunk.
     */
    fun upsertNlg(devId: String, roleId: String, bizId: String, content: String?, ts: Long): Long {
        return try {
            val db = writableDatabase
            val v = ContentValues().apply {
                put(C_CONTENT, content)
                put(C_TS, ts)
            }
            val rows = db.update(
                T, v,
                "$C_DEV=? AND $C_ROLE=? AND $C_BIZ=? AND $C_SENDER=1 AND $C_TYPE=?",
                arrayOf(devId, roleId, bizId, "nlg_text")
            )
            if (rows > 0) {
                rows.toLong()
            } else {
                insert(
                    ChatMessageRecord(
                        devId = devId, roleId = roleId, bizId = bizId, sender = 1,
                        msgType = "nlg_text", content = content, imageUri = null, ts = ts
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "upsertNlg error: ${e.message}")
            -1
        }
    }

    fun query(devId: String, roleId: String, limit: Int = 200): List<ChatMessageRecord> {
        val list = ArrayList<ChatMessageRecord>()
        try {
            readableDatabase.query(
                T, null, "$C_DEV=? AND $C_ROLE=?", arrayOf(devId, roleId),
                null, null, "$C_TS ASC", limit.toString()
            ).use { c ->
                while (c.moveToNext()) {
                    list.add(
                        ChatMessageRecord(
                            id = c.getLong(c.getColumnIndexOrThrow(C_ID)),
                            devId = devId,
                            roleId = roleId,
                            bizId = c.getString(c.getColumnIndexOrThrow(C_BIZ)),
                            sender = c.getInt(c.getColumnIndexOrThrow(C_SENDER)),
                            msgType = c.getString(c.getColumnIndexOrThrow(C_TYPE)),
                            content = c.getString(c.getColumnIndexOrThrow(C_CONTENT)),
                            imageUri = c.getString(c.getColumnIndexOrThrow(C_IMG)),
                            ts = c.getLong(c.getColumnIndexOrThrow(C_TS))
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "query error: ${e.message}")
        }
        return list
    }

    fun deleteByRole(devId: String, roleId: String): Int {
        return try {
            writableDatabase.delete(T, "$C_DEV=? AND $C_ROLE=?", arrayOf(devId, roleId))
        } catch (e: Exception) {
            Log.e(TAG, "deleteByRole error: ${e.message}")
            -1
        }
    }
}
