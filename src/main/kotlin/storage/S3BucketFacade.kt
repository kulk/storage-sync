package storage

import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Singleton
import mu.KotlinLogging
import org.eclipse.microprofile.config.inject.ConfigProperty
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

private val log = KotlinLogging.logger {}

@Singleton
class S3BucketFacade : StorageFacade {

    @ConfigProperty(name = "aws.access.key")
    lateinit var accessKey: String

    @ConfigProperty(name = "aws.secret.key")
    lateinit var secretKey: String

    @ConfigProperty(name = "aws.region", defaultValue = "eu-central-1")
    lateinit var regionName: String

    @ConfigProperty(name = "aws.s3.bucket.name")
    lateinit var bucketName: String

    private val credentials: AwsCredentials by lazy {
        AwsBasicCredentials.create(accessKey, secretKey)
    }

    private val s3Client: S3Client by lazy {
        S3Client.builder()
            .region(Region.of(regionName))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build()
    }

    override fun createStorage(): Boolean {
        try{
            val response = s3Client.createBucket { request: CreateBucketRequest.Builder ->
                request.bucket(bucketName)
            }
            log.info { "Bucket creation response: $response" }
            return response.sdkHttpResponse().statusCode() == 200

        } catch (exception: S3Exception){
            log.error { exception.awsErrorDetails() }
            return false
        }

    }

    override fun storageExists(): Boolean {
        try {
            s3Client.headBucket { request: HeadBucketRequest.Builder -> request.bucket(bucketName) }
            return true
        } catch (exception: NoSuchBucketException) {
            return false
        }
    }

    override fun deleteStorage(): Boolean {
        try {
            s3Client.deleteBucket { request: DeleteBucketRequest.Builder -> request.bucket(bucketName) }
            return true
        } catch (exception: S3Exception) {
            log.error { exception.awsErrorDetails() }
            return false
        }
    }

    override fun addFile(file: File): Boolean {
//        val timestamp = Files.getLastModifiedTime(file.toPath()).toString()
//        val metadata = mutableMapOf("lastModified" to timestamp)
        log.info { "Uploading file to S3Bucket: ${file.path}" }
        try {
            val response = s3Client.putObject(
                { request ->
                    request
                        .bucket(bucketName)
                        .key(file.path.toString())
//                        .metadata(metadata)
//                        .ifNoneMatch("*")
                },
                file.toPath()
            )
            log.info { "Successfully uploaded file to S3 Bucket: ${file.path}" }
            return response.sdkHttpResponse().statusCode() == 200

        } catch (exception: S3Exception) {
            log.error { "Failed to upload file to S3 Bucket ${file.path}. Exception: ${exception.awsErrorDetails()}" }
            return false
        }
    }

    override fun listFiles(): Map<String, ZonedDateTime> {
        val listObjectsV2Request = ListObjectsV2Request.builder()
            .bucket(bucketName)
            .build()
        val listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request)

        val contents = listObjectsV2Response.contents()

        log.info { "Number of objects in the bucket: " + contents.stream().count() }
        return contents.associate {
            it.key() to it.lastModified().toString().toAmsterdamZonedDateTime()
        }
    }

    override fun deleteFile(file: File): Boolean {
        log.info { "Deleting file from S3 Bucket: ${file.path}"}
        val objectToDelete = ObjectIdentifier
            .builder()
            .key(file.name.toString())
            .build()
        try {
            val response = s3Client.deleteObjects { request: DeleteObjectsRequest.Builder ->
                request
                    .bucket(bucketName)
                    .delete { deleteRequest: Delete.Builder ->
                        deleteRequest
                            .objects(objectToDelete)
                    }
            }
            log.info { "File has been deleted: ${file.path}" }
            return response.sdkHttpResponse().statusCode() == 200

        } catch (exception: S3Exception) {
            log.error { "Could not delete file: ${file.path}. Exception: ${exception.awsErrorDetails()}" }
            return false
        }
    }

    private fun close() {
        try {
            s3Client.close()
        } catch (e: Exception) {
            log.error(e) { "Error closing S3 client" }
        }
    }

    @PreDestroy
    fun cleanup() {
        close()
    }

}

fun String.toAmsterdamZonedDateTime(): ZonedDateTime =
    Instant.parse(this).atZone(ZoneId.of("Europe/Amsterdam"))