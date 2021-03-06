package catchla.yep.fragment

import android.os.Bundle
import android.support.v4.content.Loader
import catchla.yep.annotation.PathRecipientType
import catchla.yep.extension.account
import catchla.yep.loader.HistoricalMessagesLoader
import catchla.yep.model.Message
import catchla.yep.model.Paging

/**
 * Created by mariotaku on 15/12/10.
 */
class TopicChatListFragment : ChatListFragment() {
    override fun onLoadFinished(loader: Loader<List<Message>?>, data: List<Message>?) {
        super.onLoadFinished(loader, data)
        refreshEnabled = false
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        refreshEnabled = false
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<List<Message>?> {
        val topic = topic!!
        val recipientId = topic.circle.id
        return HistoricalMessagesLoader(context, account, PathRecipientType.CIRCLES,
                recipientId, Paging(), false, false, adapter.data)
    }

    fun addMessage(result: Message) {
        val adapterData = adapter.data
        if (adapterData is MutableList) {
            val index = adapterData.indexOfFirst { result.createdAt > it.createdAt }
            adapterData.add(index, result)
            adapter.notifyItemInserted(index)
        } else if (adapterData != null) {
            val index = adapterData.indexOfFirst { result.createdAt > it.createdAt }
            val list = adapterData.toMutableList()
            list.add(index, result)
            adapter.data = list
        } else {
            adapter.data = mutableListOf(result)
        }
    }

}
