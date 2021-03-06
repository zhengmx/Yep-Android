package catchla.yep.view

import android.accounts.Account
import android.content.Context
import android.util.AttributeSet
import android.view.View
import catchla.yep.model.DribbbleShots
import catchla.yep.model.TaskResponse
import catchla.yep.model.User
import catchla.yep.model.YepException
import catchla.yep.util.ImageLoaderWrapper
import catchla.yep.util.YepAPIFactory
import catchla.yep.util.dagger.GeneralComponentHelper
import catchla.yep.view.holder.DribbbleShotViewHolder
import kotlinx.android.synthetic.main.provider_widget_dribbble.view.*
import javax.inject.Inject

/**
 * Created by mariotaku on 16/8/3.
 */
class DribbbleProviderWidgetContainer : ProviderWidgetContainer<DribbbleShots> {
    @Inject
    lateinit var imageLoader: ImageLoaderWrapper

    lateinit var account: Account

    lateinit var user: User

    constructor(context: Context) : super(context) {
        GeneralComponentHelper.build(context).inject(this)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        GeneralComponentHelper.build(context).inject(this)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        GeneralComponentHelper.build(context).inject(this)
    }

    override fun displayData(result: TaskResponse<DribbbleShots>) {
        widgetContent.visibility = View.VISIBLE
        loadProgress.visibility = View.GONE
        result.data?.let {
            val views = arrayOf(mediaPreview0, mediaPreview1, mediaPreview2)
            views.forEachIndexed { index, view ->
                if (index < it.shots.size) {
                    val image = DribbbleShotViewHolder.getBestImage(it.shots[index].images)
                    if (image != null) {
                        imageLoader.displayProviderPreviewImage(image.url, view)
                    } else {
                        view.setImageDrawable(null)
                    }
                    view.visibility = View.VISIBLE
                } else {
                    view.visibility = View.GONE
                }
            }
        }
    }

    override fun preRequest() {
        widgetContent.visibility = View.GONE
        loadProgress.visibility = View.VISIBLE
    }

    @Throws(YepException::class)
    override fun doRequest(): DribbbleShots {
        val yep = YepAPIFactory.getInstance(context, account)
        return yep.getDribbbleShots(user.id)
    }
}
