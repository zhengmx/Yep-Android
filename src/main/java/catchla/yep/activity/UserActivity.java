package catchla.yep.activity;

import android.accounts.Account;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.apmem.tools.layouts.FlowLayout;
import org.mariotaku.abstask.library.AbstractTask;
import org.mariotaku.abstask.library.TaskStarter;
import org.mariotaku.sqliteqb.library.Expression;

import java.util.ArrayList;
import java.util.List;

import catchla.yep.Constants;
import catchla.yep.R;
import catchla.yep.loader.UserLoader;
import catchla.yep.model.Conversation;
import catchla.yep.model.ProfileUpdate;
import catchla.yep.model.Provider;
import catchla.yep.model.Skill;
import catchla.yep.model.TaskResponse;
import catchla.yep.model.User;
import catchla.yep.model.YepException;
import catchla.yep.provider.YepDataStore.Friendships;
import catchla.yep.util.JsonSerializer;
import catchla.yep.util.MenuUtils;
import catchla.yep.util.ParseUtils;
import catchla.yep.util.Utils;
import catchla.yep.util.YepAPI;
import catchla.yep.util.YepAPIFactory;
import catchla.yep.util.task.UpdateProfileTask;

public class UserActivity extends SwipeBackContentActivity implements Constants, View.OnClickListener,
        LoaderManager.LoaderCallbacks<TaskResponse<User>>, UpdateProfileTask.Callback {

    private static final int REQUEST_SELECT_MASTER_SKILLS = 111;
    private static final int REQUEST_SELECT_LEARNING_SKILLS = 112;

    private FloatingActionButton mActionButton;
    private ImageView mProfileImageView;
    private TextView mNameView;
    private TextView mUsernameView;
    private TextView mIntroductionView;
    private FlowLayout mMasterSkills, mLearningSkills;
    private View mMasterLabel, mLearningLabel;
    private LinearLayout mProvidersContainer;
    private View mProvidersDivider;
    private User mCurrentUser;
    private View mUserScrollContent;
    private View mUserScrollView;
    private AppBarLayout mAppBarLayout;
    private Toolbar mToolbar;
    private View mTopicsItemView;
    private View mCollapsingToolbar;

    private Rect mSystemWindowsInsets = new Rect();
    private UpdateProfileTask mUpdateProfileTask;

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        mActionButton = (FloatingActionButton) findViewById(R.id.fab);
        mProfileImageView = (ImageView) findViewById(R.id.profile_image);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mNameView = (TextView) findViewById(R.id.name);
        mUsernameView = (TextView) findViewById(R.id.username);
        mIntroductionView = (TextView) findViewById(R.id.introduction);
        mMasterSkills = (FlowLayout) findViewById(R.id.master_skills);
        mLearningSkills = (FlowLayout) findViewById(R.id.learning_skills);
        mMasterLabel = findViewById(R.id.master_label);
        mLearningLabel = findViewById(R.id.learning_label);
        mProvidersContainer = (LinearLayout) findViewById(R.id.providers_container);
        mProvidersDivider = findViewById(R.id.providers_divider);
        mUserScrollContent = findViewById(R.id.user_scroll_content);
        mUserScrollView = findViewById(R.id.user_scroll_view);
        mAppBarLayout = (AppBarLayout) findViewById(R.id.app_bar);
        mTopicsItemView = findViewById(R.id.user_topics);
        mCollapsingToolbar = findViewById(R.id.collapsing_toolbar);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);


        final User currentUser;
        final Intent intent = getIntent();
        final Account account = getAccount();

        if (intent.hasExtra(EXTRA_USER)) {
            currentUser = intent.getParcelableExtra(EXTRA_USER);
        } else {
            currentUser = Utils.getAccountUser(this, account);
        }

        if (currentUser == null) {
            finish();
            return;
        }
        mActionButton.setOnClickListener(this);
        mTopicsItemView.setOnClickListener(this);

        setTitle(Utils.getDisplayName(currentUser));
        displayUser(currentUser);

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    private void displayUser(final User user) {
        if (user == null) return;
        mCurrentUser = user;
        final String avatarUrl = user.getAvatarUrl();
        mImageLoader.displayProfileImage(avatarUrl, mProfileImageView);
        final String username = user.getUsername();
        final String introduction = user.getIntroduction();
        mNameView.setText(user.getNickname());
        if (TextUtils.isEmpty(username)) {
            mUsernameView.setText(R.string.no_username);
        } else {
            mUsernameView.setText(username);
        }
        if (TextUtils.isEmpty(introduction)) {
            mIntroductionView.setText(R.string.no_introduction_yet);
        } else {
            mIntroductionView.setText(introduction);
        }

        View.OnClickListener skillOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final Skill skill = (Skill) v.getTag();
                final Intent intent = new Intent(UserActivity.this, SkillUsersActivity.class);
                intent.putExtra(EXTRA_ACCOUNT, getAccount());
                intent.putExtra(EXTRA_SKILL, skill);
                startActivity(intent);
            }
        };

        final boolean isMySelf = Utils.isMySelf(this, getAccount(), user);

        if (isMySelf) {
            mActionButton.setImageResource(R.drawable.ic_action_edit);
        } else {
            mActionButton.setImageResource(R.drawable.ic_action_chat);
        }

        final LayoutInflater inflater = UserActivity.this.getLayoutInflater();

        final ArrayList<Skill> learningSkills = Utils.arrayListFrom(user.getLearningSkills());
        mLearningSkills.removeAllViews();
        if (learningSkills != null) {
            for (Skill skill : learningSkills) {
                final View view = Utils.inflateSkillItemView(UserActivity.this, inflater, skill, mLearningSkills);
                final View skillButton = view.findViewById(R.id.skill_button);
                skillButton.setTag(skill);
                skillButton.setOnClickListener(skillOnClickListener);
                mLearningSkills.addView(view);
            }
        }
        if (isMySelf) {
            mLearningLabel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    final Intent intent = new Intent(UserActivity.this, SkillSelectorActivity.class);
                    intent.putParcelableArrayListExtra(EXTRA_SKILLS, learningSkills);
                    startActivityForResult(intent, REQUEST_SELECT_LEARNING_SKILLS);
                }
            });
        } else {
            //TODO: Add empty view
        }
        final ArrayList<Skill> masterSkills = Utils.arrayListFrom(user.getMasterSkills());
        mMasterSkills.removeAllViews();
        if (masterSkills != null) {
            for (Skill skill : masterSkills) {
                final View view = Utils.inflateSkillItemView(UserActivity.this, inflater, skill, mMasterSkills);
                final View skillButton = view.findViewById(R.id.skill_button);
                skillButton.setTag(skill);
                skillButton.setOnClickListener(skillOnClickListener);
                mMasterSkills.addView(view);
            }
        }
        if (isMySelf) {
            mMasterLabel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    final Intent intent = new Intent(UserActivity.this, SkillSelectorActivity.class);
                    intent.putParcelableArrayListExtra(EXTRA_SKILLS, masterSkills);
                    startActivityForResult(intent, REQUEST_SELECT_MASTER_SKILLS);
                }
            });
        } else {
            //TODO: Add empty view
        }
        final List<Provider> providers = user.getProviders();
        mProvidersContainer.removeAllViews();

        String websiteUrl = user.getWebsiteUrl();
        final boolean hasWebsite = !TextUtils.isEmpty(websiteUrl);

        View.OnClickListener providerOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final Provider provider = (Provider) v.getTag();
                final Intent intent;
                if (provider.isSupported()) {
                    if (Provider.PROVIDER_BLOG.equals(provider.getName())) {
                        // TODO open web address
                        return;
                    }
                    intent = new Intent(UserActivity.this, ProviderContentActivity.class);
                } else if (isMySelf) {
                    if (Provider.PROVIDER_BLOG.equals(provider.getName())) {
                        // TODO open url editor
                        return;
                    }
                    intent = new Intent(UserActivity.this, ProviderOAuthActivity.class);
                } else {
                    return;
                }
                intent.putExtra(EXTRA_PROVIDER_NAME, provider.getName());
                intent.putExtra(EXTRA_USER, user);
                intent.putExtra(EXTRA_ACCOUNT, getAccount());
                startActivity(intent);
            }
        };
        if (hasWebsite || isMySelf) {
            Provider provider = new Provider("blog", hasWebsite);
            final View view = Utils.inflateProviderItemView(UserActivity.this,
                    inflater, provider, mProvidersContainer);
            view.setTag(provider);
            view.setOnClickListener(providerOnClickListener);
            mProvidersContainer.addView(view);
        }
        if (providers != null) {
            for (Provider provider : providers) {
                if (!provider.isSupported()) continue;
                final View view = Utils.inflateProviderItemView(UserActivity.this,
                        inflater, provider, mProvidersContainer);
                view.setTag(provider);
                view.setOnClickListener(providerOnClickListener);
                mProvidersContainer.addView(view);
            }
            if (isMySelf) {
                for (Provider provider : providers) {
                    if (provider.isSupported()) continue;
                    final View view = Utils.inflateProviderItemView(UserActivity.this,
                            inflater, provider, mProvidersContainer);
                    view.setTag(provider);
                    view.setOnClickListener(providerOnClickListener);
                    mProvidersContainer.addView(view);
                }
            }
        }
        if (mProvidersContainer.getChildCount() > 0) {
            mProvidersDivider.setVisibility(View.VISIBLE);
        } else {
            mProvidersDivider.setVisibility(View.GONE);
        }
    }

    private void updatePalette(final Palette palette) {
        Palette.Swatch swatch = palette.getDarkVibrantSwatch();
        if (swatch == null) return;
        mToolbar.setTitleTextColor(swatch.getTitleTextColor());
        mCollapsingToolbar.setBackgroundColor(swatch.getRgb());
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.fab: {
                if (Utils.isMySelf(this, getAccount(), getCurrentUser())) {
                    final Intent intent = new Intent(this, ProfileEditorActivity.class);
                    intent.putExtra(EXTRA_ACCOUNT, getAccount());
                    startActivity(intent);
                } else {
                    final Intent intent = new Intent(this, ChatActivity.class);
                    intent.putExtra(EXTRA_CONVERSATION, Conversation.fromUser(getCurrentUser(),
                            Utils.getAccountId(this, getAccount())));
                    startActivity(intent);
                }
                break;
            }
            case R.id.user_topics: {
                final Intent intent = new Intent(this, UserTopicsActivity.class);
                intent.putExtra(EXTRA_ACCOUNT, getAccount());
                intent.putExtra(EXTRA_USER, getCurrentUser());
                startActivity(intent);
                break;
            }
        }
    }

    private User getCurrentUser() {
        return mCurrentUser;
    }

    @Override
    public Loader<TaskResponse<User>> onCreateLoader(final int id, final Bundle args) {
        return new UserLoader(this, getAccount(), getCurrentUser().getId());
    }

    @Override
    public void onLoadFinished(final Loader<TaskResponse<User>> loader, final TaskResponse<User> data) {
        if (data.hasData()) {
            final User user = data.getData();
            final Account account = getAccount();
            final String accountId = Utils.getAccountId(this, account);
            displayUser(user);
            if (StringUtils.equals(user.getId(), accountId)) {
                Utils.saveUserInfo(UserActivity.this, account, user);
            } else {
                TaskStarter.execute(new AbstractTask<Object, Object, Object>() {
                    @Override
                    public Object doLongOperation(final Object o) {
                        final ContentValues values = new ContentValues();
                        values.put(Friendships.FRIEND, JsonSerializer.serialize(user));
                        final ContentResolver cr = getContentResolver();
                        final String where = Expression.and(Expression.equalsArgs(Friendships.ACCOUNT_ID),
                                Expression.equalsArgs(Friendships.USER_ID)).getSQL();
                        cr.update(Friendships.CONTENT_URI, values, where, new String[]{accountId, user.getId()});
                        return null;
                    }
                });
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_user, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        final boolean isMySelf = Utils.isMySelf(this, getAccount(), getCurrentUser());
        MenuUtils.setMenuGroupAvailability(menu, R.id.group_menu_friend, !isMySelf);
        MenuUtils.setMenuGroupAvailability(menu, R.id.group_menu_myself, isMySelf);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings: {
                Utils.openSettings(this, getAccount());
                return true;
            }
            case R.id.share: {
                final User user = getCurrentUser();
                if (!TextUtils.isEmpty(user.getUsername())) {
                    final Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text_template,
                            Utils.getDisplayName(getCurrentUser()), Utils.getUserLink(user)));
                    startActivity(Intent.createChooser(intent, getString(R.string.share)));
                } else {
                    SetUsernameDialogFragment df = new SetUsernameDialogFragment();
                    df.show(getSupportFragmentManager(), "set_username");
                }
                return true;
            }
            case R.id.block_user: {
                TaskStarter.execute(new AbstractTask<Object, Object, Object>() {
                    @Override
                    public Object doLongOperation(final Object o) {
                        final Account currentAccount = getAccount();
                        final YepAPI yep = YepAPIFactory.getInstance(UserActivity.this, currentAccount);
                        assert yep != null;
                        try {
                            yep.blockUser(getCurrentUser().getId());
                        } catch (YepException e) {
                            Log.w(LOGTAG, e);
                        }
                        return null;
                    }
                });
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLoaderReset(final Loader<TaskResponse<User>> loader) {

    }


    @Override
    protected boolean isTintBarEnabled() {
        return false;
    }


    @Override
    public void onProfileUpdated(final User user) {
        displayUser(user);
    }

    public static class SetUsernameDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {
        @NonNull
        @Override
        public Dialog onCreateDialog(final Bundle savedInstanceState) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(R.string.set_username);
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.setPositiveButton(android.R.string.ok, this);
            builder.setView(R.layout.dialog_set_username);
            return builder.create();
        }

        @Override
        public void onClick(final DialogInterface dialog, final int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE: {
                    final EditText editUsername = (EditText) ((Dialog) dialog).findViewById(R.id.edit_username);
                    ((UserActivity) getActivity()).setUsername(ParseUtils.parseString(editUsername.getText()));
                    break;
                }
            }
        }
    }

    private void setUsername(final String username) {
        ProfileUpdate update = new ProfileUpdate();
        update.setUsername(username);
        if (mUpdateProfileTask == null || mUpdateProfileTask.getStatus() != AsyncTask.Status.RUNNING) {
            mUpdateProfileTask = new UpdateProfileTask(this, getAccount(), update, null);
            mUpdateProfileTask.execute();
        }
    }
}
