package jobs

import io.quarkus.runtime.Startup
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jdk.jfr.internal.test.DeprecatedMethods.counter
import mu.KotlinLogging
import org.eclipse.microprofile.config.inject.ConfigProperty
import service.getAllFiles
import storage.S3BucketFacade
import storage.StorageFacade
import storage.StorageFacadeFactory
import java.io.File
import java.nio.file.Files
import java.time.ZoneId
import java.time.ZonedDateTime


private val log = KotlinLogging.logger {}


@ApplicationScoped
class SyncJob {

    @ConfigProperty(name = "storage.provider")
    lateinit var storageProvider: String

    @ConfigProperty(name = "local.sync.dir")
    lateinit var localSyncDir: String

//    @Inject
//    lateinit var storageFacadeFactory: StorageFacadeFactory

    @Inject
    lateinit var s3BucketFacade: S3BucketFacade


    @Startup
    fun init() {

        val storageFacade = StorageFacadeFactory.createStorageFacade(storageProvider)
//        val storageFacade: StorageFacade = s3BucketFacade

        val localFiles = getAllFiles(localSyncDir)
        val remoteFiles = storageFacade.listFiles()
        val fileResources = FileResources(localFiles, remoteFiles, storageFacade)

        uploadFiles(fileResources)
        cleanUp(fileResources)
        print("debug")
    }

    /**
     * Iterate trough list of local files,
     * Files should be added to the storage except when:
     * - The file already exists in storage and is not older than the local file
     */
    private fun uploadFiles(files: FileResources) {
        files.localFiles.forEach loop@{ file ->
            if (files.remoteFiles.containsKey(file.path)) {
                val remoteFileLastModified = files.remoteFiles[file.path]
                if (remoteFileLastModified != null && remoteFileLastModified >= file.lastModifiedTime()) {
                    return@loop
                }
            }
            log.info { "File will be added to storage: ${file.path}" }
            files.storageFacade.addFile(file)
        }
    }

    /**
     * Delete files which are in storage but no longer in the local file system.
     */
    private fun cleanUp(files: FileResources) {
        files.remoteFiles.forEach { (remoteFile: String) ->
            val localFilesMap: Map<String, String> = files.localFiles.toMap()
            if (!localFilesMap.containsKey(remoteFile)) {
                log.info { "Remote file does not exist locally. Submitting file for deletion: $remoteFile" }
                files.storageFacade.deleteFile(File(remoteFile))
            }
        }
    }

}

fun List<File>.toMap() = this.associate { it.path to "" }

fun File.lastModifiedTime(): ZonedDateTime =
    Files.getLastModifiedTime(this.toPath()).toInstant().atZone(ZoneId.of("Europe/Amsterdam"))

private data class FileResources(
    val localFiles: List<File>,
    val remoteFiles: Map<String, ZonedDateTime>,
    val storageFacade: StorageFacade,
)