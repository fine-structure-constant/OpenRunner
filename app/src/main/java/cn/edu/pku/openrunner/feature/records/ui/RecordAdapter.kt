package cn.edu.pku.openrunner.feature.records.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cn.edu.pku.openrunner.R
import cn.edu.pku.openrunner.feature.records.domain.RecordListItem
import cn.edu.pku.openrunner.feature.records.domain.RecordUploadState
import com.google.android.material.card.MaterialCardView
import java.text.DateFormat

class RecordAdapter(
    private val onUpload: (String) -> Unit,
    private val onChoosePhoto: (String) -> Unit,
    private val onDelete: (RecordListItem) -> Unit,
    private val onOpenDetails: (RecordListItem) -> Unit
) : ListAdapter<RecordListItem, RecordAdapter.RecordViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        return RecordViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_record, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(getItem(position), onUpload, onChoosePhoto, onDelete, onOpenDetails)
    }

    class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView as MaterialCardView
        private val distance: TextView = itemView.findViewById(R.id.record_distance)
        private val date: TextView = itemView.findViewById(R.id.record_date)
        private val detail: TextView = itemView.findViewById(R.id.record_detail)
        private val status: TextView = itemView.findViewById(R.id.record_status)
        private val detailsAvailable: TextView = itemView.findViewById(
            R.id.record_details_available
        )
        private val photoStatus: TextView = itemView.findViewById(R.id.record_photo_status)
        private val upload: View = itemView.findViewById(R.id.record_upload)
        private val photo: TextView = itemView.findViewById(R.id.record_photo)
        private val delete: View = itemView.findViewById(R.id.record_delete)

        fun bind(
            item: RecordListItem,
            onUpload: (String) -> Unit,
            onChoosePhoto: (String) -> Unit,
            onDelete: (RecordListItem) -> Unit,
            onOpenDetails: (RecordListItem) -> Unit
        ) {
            val record = item.record
            val context = itemView.context
            distance.text = itemView.context.getString(R.string.record_distance, record.distance)
            date.text = record.date?.let {
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(it)
            } ?: itemView.context.getString(R.string.record_date_unknown)
            detail.text = itemView.context.getString(
                R.string.record_duration,
                record.duration.toInt(),
                record.step
            )
            val localId = item.localId
            val canUpload = localId != null && item.uploadState in setOf(
                RecordUploadState.PENDING,
                RecordUploadState.FAILED
            )
            val isAttachingPhoto = item.isPhotoProcessing
            status.text = when (item.uploadState) {
                RecordUploadState.PENDING -> context.getString(R.string.record_pending)
                RecordUploadState.UPLOADING -> context.getString(R.string.record_uploading)
                RecordUploadState.UPLOADED_VALID -> context.getString(
                    if (record.morningBonus) {
                        R.string.record_uploaded_valid_bonus
                    } else {
                        R.string.record_uploaded_valid
                    }
                )
                RecordUploadState.UPLOADED_INVALID -> context.getString(
                    R.string.record_uploaded_invalid,
                    context.recordIssueText(record.invalidReason)
                )
                RecordUploadState.FAILED -> context.getString(
                    R.string.record_upload_failed,
                    context.recordIssueText(item.failureCode, item.failureMessage)
                )
            }
            status.visibility = View.VISIBLE
            val (containerColor, accentColor) = when (item.uploadState) {
                RecordUploadState.PENDING -> R.color.or_status_pending_container to R.color.or_status_pending
                RecordUploadState.UPLOADING -> R.color.or_status_uploading_container to R.color.or_status_uploading
                RecordUploadState.UPLOADED_VALID -> R.color.or_status_success_container to R.color.or_status_success
                RecordUploadState.UPLOADED_INVALID -> R.color.or_status_error_container to R.color.or_status_error
                RecordUploadState.FAILED -> R.color.or_status_uploading_container to R.color.or_status_uploading
            }
            card.setCardBackgroundColor(ContextCompat.getColor(context, containerColor))
            card.strokeColor = ContextCompat.getColor(context, accentColor)
            card.strokeWidth = dp(if (item.hasLocalDetails) 4 else 1)
            card.isClickable = item.hasLocalDetails
            card.isFocusable = item.hasLocalDetails
            card.setOnClickListener(if (item.hasLocalDetails) {
                View.OnClickListener { onOpenDetails(item) }
            } else {
                null
            })
            status.setTextColor(ContextCompat.getColor(context, accentColor))
            detailsAvailable.visibility = if (item.hasLocalDetails) View.VISIBLE else View.GONE

            photoStatus.visibility = if (item.hasPhoto) View.VISIBLE else View.GONE
            photoStatus.setText(
                if (record.uploaded) R.string.record_photo_uploaded else R.string.record_photo_attached
            )
            upload.visibility = if (canUpload || item.uploadState == RecordUploadState.UPLOADING) {
                View.VISIBLE
            } else {
                View.GONE
            }
            upload.isEnabled = canUpload
            photo.visibility = if (canUpload) View.VISIBLE else View.GONE
            photo.isEnabled = canUpload && !isAttachingPhoto
            photo.setText(
                if (isAttachingPhoto) {
                    R.string.record_photo_processing
                } else if (item.hasPhoto) {
                    R.string.record_photo_replace
                } else {
                    R.string.record_photo_add
                }
            )
            if (canUpload) {
                val editableLocalId = checkNotNull(localId)
                upload.setOnClickListener { onUpload(editableLocalId) }
                photo.setOnClickListener { onChoosePhoto(editableLocalId) }
            } else {
                upload.setOnClickListener(null)
                photo.setOnClickListener(null)
            }
            delete.isEnabled = item.uploadState != RecordUploadState.UPLOADING && !isAttachingPhoto
            delete.setOnClickListener { onDelete(item) }
        }

        private fun dp(value: Int): Int =
            (value * itemView.resources.displayMetrics.density).toInt()
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<RecordListItem>() {
            override fun areItemsTheSame(oldItem: RecordListItem, newItem: RecordListItem): Boolean {
                return oldItem.itemKey == newItem.itemKey
            }

            override fun areContentsTheSame(
                oldItem: RecordListItem,
                newItem: RecordListItem
            ): Boolean = oldItem == newItem
        }
    }
}
