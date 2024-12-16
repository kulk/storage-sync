package storage

import java.io.File

interface StorageFacade {

    fun createStorage(): Boolean

    fun storageExists(): Boolean

    fun deleteStorage(): Boolean

    fun addFile(file: File): Boolean

    fun listFiles(): List<String>

    fun deleteFile(file: File): Boolean

}