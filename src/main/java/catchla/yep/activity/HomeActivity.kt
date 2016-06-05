/*
 * Copyright (c) 2015. Catch Inc,
 */

package catchla.yep.activity

import android.accounts.Account
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceActivity
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v4.view.MenuItemCompat
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.View
import catchla.yep.Constants
import catchla.yep.R
import catchla.yep.activity.iface.IAccountActivity
import catchla.yep.activity.iface.IControlBarActivity
import catchla.yep.adapter.TabsAdapter
import catchla.yep.fragment.*
import catchla.yep.fragment.iface.IActionButtonSupportFragment
import catchla.yep.fragment.iface.RefreshScrollTopInterface
import catchla.yep.menu.HomeMenuActionProvider
import catchla.yep.service.MessageService
import catchla.yep.util.ThemeUtils
import catchla.yep.util.Utils
import catchla.yep.view.FloatingActionMenu
import catchla.yep.view.TabPagerIndicator
import catchla.yep.view.TintedStatusFrameLayout
import catchla.yep.view.iface.PagerIndicator

/**
 * Created by mariotaku on 15/4/29.
 */
class HomeActivity : AppCompatActivity(), Constants, IAccountActivity, ViewPager.OnPageChangeListener, View.OnClickListener, PagerIndicator.TabListener, IControlBarActivity {

    private lateinit var adapter: TabsAdapter

    private lateinit var viewPager: ViewPager
    private lateinit var floatingActionButton: FloatingActionButton
    private lateinit var floatingActionMenu: FloatingActionMenu
    private lateinit var mPagerIndicator: TabPagerIndicator
    private lateinit var mainContent: TintedStatusFrameLayout

    override fun onContentChanged() {
        super.onContentChanged()
        mainContent = findViewById(R.id.main_content) as TintedStatusFrameLayout
        viewPager = findViewById(R.id.view_pager) as ViewPager
        floatingActionButton = findViewById(R.id.floating_action_button) as FloatingActionButton
        floatingActionMenu = findViewById(R.id.floating_action_menur) as FloatingActionMenu
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        val actionBar = supportActionBar!!
        val provider = HomeMenuActionProvider(actionBar.themedContext)
        val account = Utils.getCurrentAccount(this)
        provider.setAccount(account!!)
        provider.setOnActionListener(object : HomeMenuActionProvider.OnActionListener {
            override fun onProfileClick() {
                val intent = Intent(this@HomeActivity, UserActivity::class.java)
                intent.putExtra(Constants.EXTRA_ACCOUNT, account)
                startActivity(intent)
            }

            override fun onActionClick(action: HomeMenuActionProvider.Action) {
                when (action.id) {
                    R.id.settings -> {
                        Utils.openSettings(this@HomeActivity, account)
                    }
                    R.id.about -> {
                        startActivity(Intent(this@HomeActivity, AboutActivity::class.java))
                    }
                    R.id.development -> {
                        val intent = Intent(this@HomeActivity, SettingsActivity::class.java)
                        intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsDetailsFragment::class.java.name)
                        val args = Bundle()
                        args.putInt(Constants.EXTRA_RESID, R.xml.pref_dev)
                        intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS, args)
                        startActivity(intent)
                    }
                }
            }
        })
        MenuItemCompat.setActionProvider(menu.findItem(R.id.menu), provider)
        return true
    }

    override fun onDestroy() {
        viewPager.removeOnPageChangeListener(this)

        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val actionBar = supportActionBar!!
        actionBar.setDisplayShowCustomEnabled(true)
        actionBar.setCustomView(R.layout.layout_home_tabs)
        val primaryColor = ThemeUtils.getColorFromAttribute(this, R.attr.colorPrimary, 0)
        actionBar.setBackgroundDrawable(ThemeUtils.getActionBarBackground(primaryColor, true))
        mPagerIndicator = actionBar.customView.findViewById(R.id.pager_indicator) as TabPagerIndicator
        setContentView(R.layout.activity_home)
        val toolbar = window.findViewById(android.support.v7.appcompat.R.id.action_bar) as Toolbar
        toolbar.setContentInsetsRelative(resources.getDimensionPixelSize(R.dimen.element_spacing_normal), 0)
        adapter = TabsAdapter(actionBar.themedContext, supportFragmentManager)
        adapter.tabListener = this
        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 2
        viewPager.addOnPageChangeListener(this)
        mainContent.setStatusBarColor(primaryColor)
        floatingActionButton.setOnClickListener(this)

        val args = Bundle()
        args.putBoolean(Constants.EXTRA_CACHING_ENABLED, true)
        val account = account
        args.putParcelable(Constants.EXTRA_ACCOUNT, account)

        adapter.addTab(ConversationsListFragment::class.java, getString(R.string.tab_title_chats), R.drawable.ic_action_chat, args)
        adapter.addTab(FriendsListFragment::class.java, getString(R.string.tab_title_friends), R.drawable.ic_action_contact, args)
        adapter.addTab(TopicsListFragment::class.java, getString(R.string.topics), R.drawable.ic_action_feeds, args)
        adapter.addTab(DiscoverFragment::class.java, getString(R.string.tab_title_explore), R.drawable.ic_action_explore, args)
        mPagerIndicator.setViewPager(viewPager)
        mPagerIndicator.updateAppearance()

        updateActionButton()

        val intent = Intent(this, MessageService::class.java)
        intent.action = MessageService.ACTION_REFRESH_FRIENDSHIPS
        intent.putExtra(Constants.EXTRA_ACCOUNT, account)
        startService(intent)
    }

    override val account: Account?
        get() = Utils.getCurrentAccount(this)

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

    }

    override fun onPageReselected(position: Int) {
        val fragment = currentFragment
        if (fragment is RefreshScrollTopInterface) {
            fragment.scrollToStart()
        }
    }

    override fun onPageSelected(position: Int) {
        updateActionButton()
    }

    override fun onTabLongClick(position: Int): Boolean {
        return false
    }

    private fun updateActionButton() {
        val currentFragment = currentFragment
        if (currentFragment is IActionButtonSupportFragment) {
            floatingActionButton.setImageResource(currentFragment.actionIcon)
            floatingActionButton.show()
            val actionMenuFragmentCls = currentFragment.actionMenuFragment
            val fm = supportFragmentManager
            val ft = fm.beginTransaction()
            if (actionMenuFragmentCls != null) {
                floatingActionMenu.visibility = View.VISIBLE
                val actionMenuFragment = Fragment.instantiate(this, actionMenuFragmentCls.name) as FloatingActionMenuFragment
                actionMenuFragment.belongsTo = currentFragment
                ft.replace(floatingActionMenu.id, actionMenuFragment)
            } else {
                val currentMenuFragment = fm.findFragmentById(floatingActionMenu.id)
                if (currentMenuFragment != null) {
                    ft.remove(currentMenuFragment)
                }
                floatingActionMenu.visibility = View.GONE
            }
            ft.commit()
        } else {
            floatingActionButton.hide()
            floatingActionMenu.visibility = View.GONE
        }
    }

    private val currentFragment: Fragment?
        get() = adapter.instantiateItem(viewPager, viewPager.currentItem) as Fragment?

    override fun onPageScrollStateChanged(state: Int) {

    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.floating_action_button -> {
                val fragment = currentFragment
                if (fragment is IActionButtonSupportFragment) {
                    fragment.onActionPerformed()
                }
            }
        }
    }

    override fun setControlBarOffset(offset: Float) {

    }

    override fun setControlBarVisibleAnimate(visible: Boolean) {
        val currentFragment = currentFragment
        if (currentFragment is IActionButtonSupportFragment) {
            if (visible) {
                floatingActionButton.show()
                if (currentFragment.actionMenuFragment != null) {
                    floatingActionMenu.show()
                } else {
                    floatingActionMenu.hide()
                }
            } else {
                floatingActionButton.hide()
                floatingActionMenu.hide()
            }
        } else {
            floatingActionButton.hide()
            floatingActionMenu.hide()
        }
    }

    override fun getControlBarOffset(): Float {
        return 0f
    }

    override fun getControlBarHeight(): Int {
        return 0
    }

    override fun notifyControlBarOffsetChanged() {

    }

    override fun registerControlBarOffsetListener(listener: IControlBarActivity.ControlBarOffsetListener) {

    }

    override fun unregisterControlBarOffsetListener(listener: IControlBarActivity.ControlBarOffsetListener) {

    }
}
