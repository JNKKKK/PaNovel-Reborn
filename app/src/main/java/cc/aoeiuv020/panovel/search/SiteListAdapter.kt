package cc.aoeiuv020.panovel.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.data.availability.AvailabilityManager
import cc.aoeiuv020.panovel.data.availability.ProbeRecord
import cc.aoeiuv020.panovel.data.entity.Site
import cc.aoeiuv020.panovel.util.hide
import cc.aoeiuv020.panovel.util.show
import cc.aoeiuv020.panovel.util.tip

class SiteListAdapter(
        siteList: List<Site>,
        private val itemListener: ItemListener
) : androidx.recyclerview.widget.RecyclerView.Adapter<SiteListAdapter.ViewHolder>() {
    private val data: MutableList<Site> = siteList.toMutableList()

    // 书源名 -> 可用性历史，随探测完成刷新，
    private var history: Map<String, List<ProbeRecord>> = emptyMap()

    override fun getItemCount(): Int {
        return data.size
    }

    /** 探测数据更新后整表重绑状态条， */
    fun updateHistory(history: Map<String, List<ProbeRecord>>) {
        this.history = history
        notifyItemRangeChanged(0, data.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.site_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(data[position])
    }

    inner class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        private val tvStopUpkeep: TextView = itemView.findViewById(R.id.tvStopUpkeep)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvUrl: TextView = itemView.findViewById(R.id.tvUrl)
        private val availabilityBars: AvailabilityBarsView =
            itemView.findViewById(R.id.availabilityBars)
        val cbEnabled: CheckBox = itemView.findViewById(R.id.cbEnabled)
        lateinit var site: Site

        init {
            cbEnabled.setOnCheckedChangeListener { _, checked: Boolean ->
                if (site.enabled == checked) {
                    return@setOnCheckedChangeListener
                }
                site.enabled = checked
                itemListener.onEnabledChanged(site)
            }
            itemView.setOnClickListener {
                itemListener.onSiteSelect(site)
            }
            itemView.setOnLongClickListener {
                itemListener.onItemLongClick(this)
            }
        }

        fun bind(data: Site) {
            this.site = data
            tvName.text = data.name
            cbEnabled.isChecked = data.enabled
            val novelContext = DataManager.api.getNovelContextByName(data.name)
            tvUrl.text = novelContext.homePage
            if (novelContext.upkeep) {
                tvStopUpkeep.hide()
            } else {
                tvStopUpkeep.show()
                tvStopUpkeep.setOnClickListener { v ->
                    v.context.tip(novelContext.reason.takeIf { it.isNotEmpty() }
                        ?: v.context.getString(R.string.tip_stop_upkeep_reason_default))
                }
            }
            availabilityBars.setStatuses(
                AvailabilityManager.barsFor(history[data.name], BAR_SLOTS)
            )
        }
    }

    interface ItemListener {
        fun onEnabledChanged(site: Site)
        fun onSiteSelect(site: Site)
        fun onItemLongClick(vh: ViewHolder): Boolean
    }

    companion object {
        // 面板展示最近多少天的状态条，
        private const val BAR_SLOTS = 14
    }

}