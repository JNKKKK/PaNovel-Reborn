package cc.aoeiuv020.panovel.util

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.module.AppGlideModule
import timber.log.Timber
import java.io.InputStream
import java.net.URL


// Glide 5 removed manifest-based GlideModule parsing; registration is now via the
// @GlideModule annotation on an AppGlideModule, processed by glide's KSP compiler.
@GlideModule
class JarGlideModule : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        Timber.i("Glide registerComponents: JarGlideModule")
        registry.prepend(GlideUrl::class.java, InputStream::class.java, JarFactory())
    }

    // Manifest parsing is disabled since we use the annotation-based API.
    override fun isManifestParsingEnabled(): Boolean = false
}


private class JarFactory : ModelLoaderFactory<GlideUrl, InputStream> {
    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<GlideUrl, InputStream> {
        return JarLoader()
    }

    override fun teardown() {
        // 不知道要不要做什么，
    }
}

private class JarLoader : ModelLoader<GlideUrl, InputStream> {
    override fun buildLoadData(model: GlideUrl, width: Int, height: Int, options: Options): ModelLoader.LoadData<InputStream>? {
        Timber.d("load $model")
        return ModelLoader.LoadData(model, UrlStreamFetcher(model.toURL()))
    }

    override fun handles(model: GlideUrl): Boolean {
        return try {
            model.toURL().protocol == "jar"
        } catch (e: Exception) {
            false
        }
    }
}

private class UrlStreamFetcher(
        private val model: URL
) : DataFetcher<InputStream> {
    private lateinit var inputStream: InputStream
    override fun getDataClass(): Class<InputStream> {
        return InputStream::class.java
    }

    override fun cleanup() {
        if (::inputStream.isInitialized) {
            try {
                inputStream.close()
            } catch (_: Exception) {
            }
        }
    }

    override fun getDataSource(): DataSource {
        return DataSource.REMOTE
    }

    override fun cancel() {
    }

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        Timber.d("open $model")
        try {
            inputStream = model.openStream()
        } catch (e: Exception) {
            callback.onLoadFailed(e)
            return
        }
        // 不清楚流程，但是以防万一，模仿HttpUrlFetcher把成功的回调放在外面，
        callback.onDataReady(inputStream)
    }
}