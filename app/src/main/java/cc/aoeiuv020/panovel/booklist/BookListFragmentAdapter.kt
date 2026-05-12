package cc.aoeiuv020.panovel.booklist

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.data.entity.BookList

class BookListFragmentAdapter(
        private val itemListener: ItemListener
) : RecyclerView.Adapter<BookListFragmentAdapter.ViewHolder>() {
    private var _data: List<BookList> = emptyList()
    var data: List<BookList>
        get() = _data
        set(value) {
            val oldData = _data
            _data = value
            DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize() = oldData.size
                override fun getNewListSize() = value.size
                override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                    oldData[oldPos].nId == value[newPos].nId
                override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                    oldData[oldPos].name == value[newPos].name
            }).dispatchUpdatesTo(this)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.book_list_item, parent, false)
        return ViewHolder(itemView, itemListener)
    }

    override fun getItemCount(): Int = _data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.apply(_data[position])
    }

    class ViewHolder(itemView: View, itemListener: ItemListener) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.ivName)
        private val count: TextView = itemView.findViewById(R.id.ivCount)
        lateinit var bookList: BookList
            private set
        val context: Context = itemView.context

        init {
            itemView.setOnClickListener {
                itemListener.onClick(this)
            }
            itemView.setOnLongClickListener {
                itemListener.onLongClick(this)
            }
        }

        fun apply(bookList: BookList) {
            this.bookList = bookList
            name.text = bookList.name
            count.text = ""
        }
    }

    interface ItemListener {
        fun onClick(vh: ViewHolder)
        fun onLongClick(vh: ViewHolder): Boolean
    }
}