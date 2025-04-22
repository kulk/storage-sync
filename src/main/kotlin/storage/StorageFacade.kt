package storage

import java.io.File
import java.time.ZonedDateTime

interface StorageFacade {

    fun createStorage(): Boolean

    fun storageExists(): Boolean

    fun deleteStorage(): Boolean

    fun addFile(file: File): Boolean

    fun listFiles(): Map<String, ZonedDateTime>

    fun deleteFile(file: File): Boolean

}