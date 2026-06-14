package com.cristal.bristral.tristal.mistral.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cristal.bristral.tristal.mistral.R
import com.cristal.bristral.tristal.mistral.model.AppInfo

class AppGridAdapter(
    private val context: android.content.Context,
    private var apps: MutableList<AppInfo>,
    private val listener: OnAppClickListener
) : RecyclerView.Adapter<AppGridAdapter.AppViewHolder>() {

    interface OnAppClickListener {
        fun onAppClick(appInfo: AppInfo)
        fun onAppLongClick(appInfo: AppInfo)
    }

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.iv_app_icon)
        val tvName: TextView = itemView.findViewById(R.id.tv_app_name)

        fun bind(appInfo: AppInfo) {
            ivIcon.setImageDrawable(appInfo.icon)
            tvName.text = appInfo.name
            itemView.setOnClickListener { listener.onAppClick(appInfo) }
            itemView.setOnLongClickListener { listener.onAppLongClick(appInfo); true }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(apps[position])
    }

    override fun getItemCount(): Int = apps.size

    fun updateList(newList: MutableList<AppInfo>) {
        apps = newList
        notifyDataSetChanged()
    }
}
