package catchla.yep.view.holder

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.TextView

import catchla.yep.R
import catchla.yep.adapter.TopicsAdapter
import catchla.yep.model.GithubAttachment
import catchla.yep.model.Topic
import catchla.yep.util.ImageLoaderWrapper

/**
 * Created by mariotaku on 15/12/9.
 */
class GithubTopicViewHolder(topicsAdapter: TopicsAdapter, itemView: View, context: Context,
                            imageLoader: ImageLoaderWrapper,
                            listener: TopicsAdapter.TopicClickListener?) : TopicViewHolder(topicsAdapter, itemView, context, imageLoader, listener) {

    private val repoName: TextView
    private val repoDescription: TextView

    init {
        repoName = itemView.findViewById(R.id.repo_name) as TextView
        repoDescription = itemView.findViewById(R.id.repo_description) as TextView
        itemView.findViewById(R.id.attachment_view).setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.attachment_view -> {
                val attachment = adapter.getTopic(layoutPosition).attachments[0] as GithubAttachment
                adapter.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(attachment.url)))
                return
            }
        }
        super.onClick(v)
    }

    override fun displayTopic(topic: Topic) {
        super.displayTopic(topic)
        val attachment = topic.attachments[0] as GithubAttachment
        repoName.text = attachment.name
        repoDescription.text = attachment.description
    }
}