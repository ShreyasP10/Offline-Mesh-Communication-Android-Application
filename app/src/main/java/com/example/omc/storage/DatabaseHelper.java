package com.example.omc.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "omc_mesh.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_MESSAGES = "messages";
    public static final String COL_MSG_ID = "message_id";
    public static final String COL_SOURCE_ID = "source_id";
    public static final String COL_SENDER_NAME = "sender_name";
    public static final String COL_DEST_ID = "destination_id";
    public static final String COL_TEXT = "text";
    public static final String COL_STATUS = "status";
    public static final String COL_TIMESTAMP = "timestamp";
    public static final String COL_IS_OUTGOING = "is_outgoing";
    public static final String COL_HOP_COUNT = "hop_count";

    public static final String TABLE_SETTINGS = "settings";
    public static final String COL_SETTING_KEY = "key";
    public static final String COL_SETTING_VAL = "val";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createMessagesTable = "CREATE TABLE " + TABLE_MESSAGES + " ("
                + COL_MSG_ID + " TEXT PRIMARY KEY, "
                + COL_SOURCE_ID + " TEXT, "
                + COL_SENDER_NAME + " TEXT, "
                + COL_DEST_ID + " TEXT, "
                + COL_TEXT + " TEXT, "
                + COL_STATUS + " TEXT, "
                + COL_TIMESTAMP + " INTEGER, "
                + COL_IS_OUTGOING + " INTEGER, "
                + COL_HOP_COUNT + " INTEGER"
                + ")";

        String createSettingsTable = "CREATE TABLE " + TABLE_SETTINGS + " ("
                + COL_SETTING_KEY + " TEXT PRIMARY KEY, "
                + COL_SETTING_VAL + " TEXT"
                + ")";

        db.execSQL(createMessagesTable);
        db.execSQL(createSettingsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTINGS);
        onCreate(db);
    }

    public synchronized void saveMessage(ChatMessage message) {
        if (message == null) return;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_MSG_ID, message.getMessageId());
        values.put(COL_SOURCE_ID, message.getSourceId());
        values.put(COL_SENDER_NAME, message.getSenderName());
        values.put(COL_DEST_ID, message.getDestinationId());
        values.put(COL_TEXT, message.getText());
        values.put(COL_STATUS, message.getStatus());
        values.put(COL_TIMESTAMP, message.getTimestamp());
        values.put(COL_IS_OUTGOING, message.isOutgoing() ? 1 : 0);
        values.put(COL_HOP_COUNT, message.getHopCount());

        db.insertWithOnConflict(TABLE_MESSAGES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public synchronized void updateMessageStatus(String messageId, String status) {
        if (messageId == null) return;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_STATUS, status);
        db.update(TABLE_MESSAGES, values, COL_MSG_ID + " = ?", new String[]{messageId});
    }

    public synchronized List<ChatMessage> getMessagesForConversation(String peerNodeId, String localNodeId) {
        List<ChatMessage> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        String selection;
        String[] selectionArgs;

        if (ChatMessage.BROADCAST_DESTINATION.equalsIgnoreCase(peerNodeId)) {
            selection = COL_DEST_ID + " = ?";
            selectionArgs = new String[]{ChatMessage.BROADCAST_DESTINATION};
        } else {
            // Unicast: either sent from local to peer, or from peer to local
            selection = "(" + COL_SOURCE_ID + " = ? AND " + COL_DEST_ID + " = ?) OR ("
                    + COL_SOURCE_ID + " = ? AND " + COL_DEST_ID + " = ?)";
            selectionArgs = new String[]{localNodeId, peerNodeId, peerNodeId, localNodeId};
        }

        Cursor cursor = db.query(TABLE_MESSAGES, null, selection, selectionArgs, null, null, COL_TIMESTAMP + " ASC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToMessage(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public synchronized List<ChatMessage> getPendingMessages() {
        List<ChatMessage> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_MESSAGES,
                null,
                COL_STATUS + " = ?",
                new String[]{ChatMessage.STATUS_PENDING},
                null, null,
                COL_TIMESTAMP + " ASC"
        );
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToMessage(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public synchronized List<ChatMessage> getAllMessages() {
        List<ChatMessage> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_MESSAGES, null, null, null, null, null, COL_TIMESTAMP + " DESC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToMessage(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public synchronized void purgeExpired(long olderThanTimestamp) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_MESSAGES, COL_TIMESTAMP + " < ?", new String[]{String.valueOf(olderThanTimestamp)});
    }

    public synchronized void saveSetting(String key, String value) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_SETTING_KEY, key);
        values.put(COL_SETTING_VAL, value);
        db.insertWithOnConflict(TABLE_SETTINGS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public synchronized String getSetting(String key, String defaultValue) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_SETTINGS,
                new String[]{COL_SETTING_VAL},
                COL_SETTING_KEY + " = ?",
                new String[]{key},
                null, null, null
        );
        String result = defaultValue;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                result = cursor.getString(0);
            }
            cursor.close();
        }
        return result;
    }

    private ChatMessage cursorToMessage(Cursor cursor) {
        return new ChatMessage(
                cursor.getString(cursor.getColumnIndexOrThrow(COL_MSG_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_SOURCE_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_SENDER_NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_DEST_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_TEXT)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_STATUS)),
                cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIMESTAMP)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_OUTGOING)) == 1,
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_HOP_COUNT))
        );
    }
}
