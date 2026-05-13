package cc.aoeiuv020.panovel

import kotlinx.coroutines.*
import timber.log.Timber

/**
 * mvp的presenter,
 * Created by AoEiuV020 on 2017.10.11-15:32:17.
 */
abstract class Presenter<T : MvpView> {
    protected var scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        private set

    var view: T? = null
        private set

    fun attach(view: MvpView) {
        Timber.v("$this attach $view")
        scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        @Suppress("UNCHECKED_CAST")
        this.view = view as? T
    }

    fun detach() {
        Timber.v("$this detach $view")
        scope.cancel()
        view = null
    }

    override fun toString(): String = "${javaClass.simpleName}@${hashCode()}"
}