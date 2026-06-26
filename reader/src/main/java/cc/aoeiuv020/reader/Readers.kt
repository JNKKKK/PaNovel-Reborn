package cc.aoeiuv020.reader

import android.content.Context
import android.view.ViewGroup

/**
 *
 * Created by AoEiuV020 on 2017.12.01-01:16:49.
 */
object Readers {

    fun getReader(context: Context, novel: String, parent: ViewGroup, requester: TextRequester, config: ReaderConfig)
            : INovelReader = Reader(context, novel, parent, requester, config)
}
