package com.tuya.smart.android.demo.camera.utils

import android.content.Context
import android.os.StatFs
import com.facebook.cache.disk.DiskCacheConfig
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.core.ExecutorSupplier
import com.facebook.imagepipeline.core.ImagePipelineConfig
import com.facebook.imagepipeline.listener.RequestListener
import com.thingclips.imagepipeline.okhttp3.OkHttpImagePipelineConfigFactory
import com.thingclips.smart.android.common.task.ThingExecutor
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.lang.IllegalArgumentException
import java.util.HashSet
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**

 * TODO feature

 *

 * @author houqing <a href="mailto:developer@tuya.com"/>

 * @since 2021/7/26 3:37 PM

 */
class FrescoManager {
    companion object{
        fun initFresco(context: Context) {
            val defaultConfig = getDefaultConfig(context, null, null)
            initFresco(context, defaultConfig)
        }

        fun initFresco(context: Context, config: ImagePipelineConfig) {
            Fresco.initialize(context, config)
        }

        private fun getDefaultConfig(context: Context, listener: RequestListener?, diskCacheConfig: DiskCacheConfig?): ImagePipelineConfig {
            val requestListeners: HashSet<RequestListener> = HashSet<RequestListener>()
            val cacheDir = File(context.cacheDir, "okhttp3")
            val size = calculateDiskCacheSize(cacheDir)
            val okHttpClient = OkHttpClient.Builder().cache(Cache(cacheDir, size))
                .connectTimeout(0, TimeUnit.MILLISECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .writeTimeout(0, TimeUnit.MILLISECONDS).build()
            val builder = OkHttpImagePipelineConfigFactory.newBuilder(
                context.applicationContext, okHttpClient
            )
            builder.setDownsampleEnabled(false).setRequestListeners(requestListeners)
            diskCacheConfig?.let {
                builder.setMainDiskCacheConfig(it)
            }
            builder.setExecutorSupplier(object : ExecutorSupplier {
                override fun forLocalStorageRead(): Executor {
                    return ThingExecutor.getInstance().thingExecutorService
                }

                override fun forLocalStorageWrite(): Executor {
                    return ThingExecutor.getInstance().thingExecutorService
                }

                override fun forDecode(): Executor {
                    return ThingExecutor.getInstance().thingExecutorService
                }

                override fun forBackgroundTasks(): Executor {
                    return ThingExecutor.getInstance().thingExecutorService
                }

                override fun scheduledExecutorServiceForBackgroundTasks(): ScheduledExecutorService? {
                    return ThingExecutor.getInstance().thingBackupService as ScheduledExecutorService?
                }

                override fun forLightweightBackgroundTasks(): Executor {
                    return ThingExecutor.getInstance().thingExecutorService
                }

                override fun forThumbnailProducer(): Executor {
                    return ThingExecutor.getInstance().thingExecutorService
                }
            })
            return builder.build()
        }

        private const val MIN_DISK_CACHE_SIZE = 5 * 1024 * 1024 // 5MB

        private const val MAX_DISK_CACHE_SIZE = 10 * 1024 * 1024 // 50MB


        private fun calculateDiskCacheSize(dir: File): Long {
            var size = MIN_DISK_CACHE_SIZE.toLong()
            try {
                val statFs = StatFs(dir.absolutePath)
                val available = statFs.blockCount.toLong() * statFs.blockSize
                // Target 2% of the total space.
                size = available / 50
            } catch (ignored: IllegalArgumentException) {
            }
            // Bound inside min/max size for disk cache.
            return Math.max(Math.min(size, MAX_DISK_CACHE_SIZE.toLong()), MIN_DISK_CACHE_SIZE.toLong())
        }
    }
}