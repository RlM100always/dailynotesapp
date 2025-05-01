package com.techtravelcoder.dailynote.room.dao

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.techtravelcoder.dailynote.room.Audio
import com.techtravelcoder.dailynote.room.BaseNote
import com.techtravelcoder.dailynote.room.Color
import com.techtravelcoder.dailynote.room.Folder
import com.techtravelcoder.dailynote.room.IdReminder
import com.techtravelcoder.dailynote.room.Image
import com.techtravelcoder.dailynote.room.ListItem
import com.techtravelcoder.dailynote.room.Reminder

@Dao
interface BaseNoteDao {

    @RawQuery
    fun query(query: SupportSQLiteQuery): Int


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(baseNote: BaseNote): Long

    @Insert
    suspend fun insert(baseNotes: List<BaseNote>)


    @Query(
        "INSERT INTO BaseNote (type, folder, color, title, pinned, timestamp, labels, body, spans, items, images, audios)\n" +
                "SELECT type, folder, color, title, pinned, timestamp, labels, body, spans, items, images, audios\n" +
                "FROM BaseNote WHERE id IN (:ids)"
    )
    suspend fun copy(ids: LongArray)


    @Query("DELETE FROM BaseNote WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM BaseNote WHERE id IN (:ids)")
    suspend fun delete(ids: LongArray)

    @Query("DELETE FROM BaseNote WHERE folder = :folder")
    suspend fun deleteFrom(folder: Folder)


    @Query("SELECT * FROM BaseNote WHERE folder = :folder ORDER BY pinned DESC, timestamp DESC")
    fun getFrom(folder: Folder): LiveData<List<BaseNote>>

    @Query("SELECT * FROM BaseNote WHERE folder = 'NOTES' ORDER BY pinned DESC, timestamp DESC")
    suspend fun getAllNotes(): List<BaseNote>

    @Query("SELECT * FROM BaseNote")
    fun getAll(): LiveData<List<BaseNote>>

    @Query("SELECT * FROM BaseNote WHERE id = :id")
    fun get(id: Long): BaseNote?

    @Query("SELECT images FROM BaseNote WHERE id = :id")
    fun getImages(id: Long): String


    @Query("SELECT images FROM BaseNote")
    fun getAllImages(): List<String>

    @Query("SELECT audios FROM BaseNote")
    fun getAllAudios(): List<String>


    @Query("SELECT id, reminder FROM BaseNote WHERE reminder IS NOT NULL")
    fun getAllReminders(): List<IdReminder>


    @Query("SELECT id FROM BaseNote WHERE folder = 'DELETED'")
    suspend fun getDeletedNoteIds(): LongArray

    @Query("SELECT id FROM BaseNote WHERE folder = 'DELETED' AND reminder IS NOT NULL")
    suspend fun getDeletedNoteReminderIds(): List<Long>

    @Query("SELECT images FROM BaseNote WHERE folder = 'DELETED'")
    suspend fun getDeletedNoteImages(): List<String>

    @Query("SELECT audios FROM BaseNote WHERE folder = 'DELETED'")
    suspend fun getDeletedNoteAudios(): List<String>


    @Query("UPDATE BaseNote SET folder = :folder WHERE id IN (:ids)")
    suspend fun move(ids: LongArray, folder: Folder)


    @Query("UPDATE BaseNote SET color = :color WHERE id IN (:ids)")
    suspend fun updateColor(ids: LongArray, color: Color)

    @Query("UPDATE BaseNote SET pinned = :pinned WHERE id IN (:ids)")
    suspend fun updatePinned(ids: LongArray, pinned: Boolean)

    @Query("UPDATE BaseNote SET labels = :labels WHERE id = :id")
    suspend fun updateLabels(id: Long, labels: List<String>)

    @Query("UPDATE BaseNote SET items = :items WHERE id = :id")
    suspend fun updateItems(id: Long, items: List<ListItem>)

    @Query("UPDATE BaseNote SET images = :images WHERE id = :id")
    suspend fun updateImages(id: Long, images: List<Image>)

    @Query("UPDATE BaseNote SET audios = :audios WHERE id = :id")
    suspend fun updateAudios(id: Long, audios: List<Audio>)

    @Query("UPDATE BaseNote SET reminder = :reminder WHERE id = :id")
    suspend fun updateReminder(id: Long, reminder: Reminder?)



    suspend fun updateChecked(id: Long, position: Int, checked: Boolean) {
        val items = requireNotNull(get(id)).items
        items[position].checked = checked
        updateItems(id, items)
    }



    fun getBaseNotesByLabel(label: String): LiveData<List<BaseNote>> {
        val result = getBaseNotesByLabel(label, Folder.NOTES)
        return Transformations.map(result) { list -> list.filter { baseNote -> baseNote.labels.contains(label) } }
    }

    @Query("SELECT * FROM BaseNote WHERE folder = :folder AND labels LIKE '%' || :label || '%' ORDER BY pinned DESC, timestamp DESC")
    fun getBaseNotesByLabel(label: String, folder: Folder): LiveData<List<BaseNote>>


    suspend fun getListOfBaseNotesByLabel(label: String): List<BaseNote> {
        val result = getListOfBaseNotesByLabelImpl(label)
        return result.filter { baseNote -> baseNote.labels.contains(label) }
    }

    @Query("SELECT * FROM BaseNote WHERE labels LIKE '%' || :label || '%'")
    suspend fun getListOfBaseNotesByLabelImpl(label: String): List<BaseNote>


    fun getBaseNotesByKeyword(keyword: String, folder: Folder): LiveData<List<BaseNote>> {
        val result = getBaseNotesByKeywordImpl(keyword, folder)
        return Transformations.map(result) { list ->
            list.filter { baseNote -> matchesKeyword(baseNote, keyword) }
        }
    }

    // Modify the SQL query to handle both cases
    @Query("""
    SELECT * FROM BaseNote 
    WHERE folder = :folder 
    AND (
        REPLACE(title, ' ', '') LIKE '%' || REPLACE(:keyword, ' ', '') || '%' 
        OR REPLACE(body, ' ', '') LIKE '%' || REPLACE(:keyword, ' ', '') || '%' 
        OR REPLACE(items, ' ', '') LIKE '%' || REPLACE(:keyword, ' ', '') || '%' 
        OR REPLACE(labels, ' ', '') LIKE '%' || REPLACE(:keyword, ' ', '') || '%'
    )
    ORDER BY pinned DESC, timestamp DESC
""")
    fun getBaseNotesByKeywordImpl(keyword: String, folder: Folder): LiveData<List<BaseNote>>

    private fun matchesKeyword(baseNote: BaseNote, keyword: String): Boolean {
        // Normalize keyword and remove spaces
        val normalizedKeyword = keyword.trim().replace("\\s+".toRegex(), " ").lowercase()
        val keywordNoSpaces = keyword.replace("\\s+".toRegex(), "").lowercase()

        // Helper function to check both conditions
        fun matches(field: String): Boolean {
            val normalizedField = field.replace("\\s+".toRegex(), " ").lowercase()
            val fieldNoSpaces = field.replace("\\s+".toRegex(), "").lowercase()
            return normalizedField.contains(normalizedKeyword) || fieldNoSpaces.contains(keywordNoSpaces)
        }

        // Check all fields
        return matches(baseNote.title) ||
                matches(baseNote.body) ||
                baseNote.labels.any { matches(it) } ||
                baseNote.items.any { matches(it.body) }
    }




}