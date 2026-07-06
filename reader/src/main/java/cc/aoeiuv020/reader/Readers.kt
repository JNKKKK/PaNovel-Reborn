package cc.aoeiuv020.reader

import android.content.Context
import android.view.ViewGroup

object Readers {

    fun getReader(context: Context, novel: String, parent: ViewGroup, requester: TextRequester, config: ReaderConfig)
            : INovelReader = Reader(context, novel, parent, requester, config)
}
