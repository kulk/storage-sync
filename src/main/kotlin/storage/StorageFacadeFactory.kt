package storage

import io.quarkus.arc.Arc
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.inject.Singleton

//@ApplicationScoped
class StorageFacadeFactory {

    companion object {
        fun createStorageFacade(provider: String): StorageFacade =
            when (provider) {
                "AWS" -> Arc.container().instance(S3BucketFacade::class.java).get()
                else -> S3BucketFacade()
            }
    }



}

//package storage
//
//import jakarta.enterprise.context.ApplicationScoped
//import jakarta.inject.Inject
//
//@ApplicationScoped
//class StorageFacadeFactory {
//
//    @Inject
//    lateinit var s3BucketFacade: S3BucketFacade
//
//    fun createStorageFacade(provider: String): StorageFacade =
//        when (provider) {
//            "AWS" -> s3BucketFacade
//            else -> S3BucketFacade()
//        }
//}