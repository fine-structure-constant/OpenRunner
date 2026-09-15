package cn.edu.pku.openrunner.feature.tasks.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cn.edu.pku.openrunner.R
import cn.edu.pku.openrunner.core.network.TaskDto

class TaskAdapter : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {
    private var items: List<TaskDto> = emptyList()

    fun submitList(tasks: List<TaskDto>) {
        items = tasks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        return TaskViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        )
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.task_name)
        private val description: TextView = itemView.findViewById(R.id.task_description)
        private val status: TextView = itemView.findViewById(R.id.task_status)

        fun bind(task: TaskDto) {
            name.text = if (task.id > 0) task.id.toString() + " " + task.name else task.name
            description.text = listOf(task.description, task.requirement)
                .filter(String::isNotBlank)
                .joinToString("\n")
            status.text = itemView.context.getString(
                if (task.isAcquired) R.string.task_acquired else R.string.task_not_acquired
            )
        }
    }
}
