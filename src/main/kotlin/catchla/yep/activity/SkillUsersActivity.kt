package catchla.yep.activity

import android.os.Bundle
import android.support.v4.view.ViewPager
import android.view.Menu
import android.view.View
import catchla.yep.Constants
import catchla.yep.Constants.*
import catchla.yep.R
import catchla.yep.adapter.TabsAdapter
import catchla.yep.extension.Bundle
import catchla.yep.extension.account
import catchla.yep.fragment.DiscoverFragment
import catchla.yep.graphic.EmptyDrawable
import catchla.yep.model.Skill
import catchla.yep.util.ThemeUtils
import catchla.yep.util.Utils
import catchla.yep.view.TabPagerIndicator
import org.mariotaku.ktextension.setItemAvailability

/**
 * Created by mariotaku on 15/6/2.
 */
class SkillUsersActivity : SwipeBackContentActivity(), Constants {

    private var mViewPager: ViewPager? = null
    private var mPagerTab: TabPagerIndicator? = null
    private var mPagerOverlay: View? = null

    private var mPagerAdapter: TabsAdapter? = null

    override fun onContentChanged() {
        super.onContentChanged()
        mViewPager = findViewById(R.id.view_pager) as ViewPager?
        mPagerTab = findViewById(R.id.view_pager_tabs) as TabPagerIndicator?
        mPagerOverlay = findViewById(R.id.pager_window_overlay)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_skill_users, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val hasSkill = Utils.hasSkill(Utils.getAccountUser(this, account), skill)
        menu.setItemAvailability(R.id.add_skill, !hasSkill)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_skill)
        mPagerAdapter = TabsAdapter(this, supportFragmentManager)
        mViewPager!!.adapter = mPagerAdapter
        mPagerTab!!.setViewPager(mViewPager)
        mPagerTab!!.updateAppearance()

        val skill = skill
        displaySkill(skill)
        val masterArgs = Bundle {
            putParcelable(EXTRA_ACCOUNT, account)
            putStringArray(EXTRA_MASTER, arrayOf(skill.id))
        }
        val learningArgs = Bundle {
            putParcelable(EXTRA_ACCOUNT, account)
            putStringArray(EXTRA_LEARNING, arrayOf(skill.id))
        }
        mPagerAdapter!!.addTab(DiscoverFragment::class.java, getString(R.string.skill_type_master), 0, masterArgs)
        mPagerAdapter!!.addTab(DiscoverFragment::class.java, getString(R.string.skill_type_learning), 0, learningArgs)


        ThemeUtils.initPagerIndicatorAsActionBarTab(this, mPagerTab, mPagerOverlay)
        ThemeUtils.setCompatToolbarOverlay(this, EmptyDrawable())
        ThemeUtils.setCompatContentViewOverlay(this, EmptyDrawable())
    }

    private val skill: Skill
        get() = intent.getParcelableExtra<Skill>(EXTRA_SKILL)

    private fun displaySkill(skill: Skill) {
        title = skill.nameString
    }

}
