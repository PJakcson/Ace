package com.aceft.ui_fragments.front_pages;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;

import com.aceft.MainActivity;
import com.aceft.R;
import com.aceft.data.AceAnims;
import com.aceft.data.Preferences;
import com.aceft.data.TwitchNetworkTasks;
import com.aceft.ui_fragments.setup.AuthFragment;
import com.aceft.ui_fragments.setup.SetupPagerFragment;

import org.json.JSONException;
import org.json.JSONObject;


public class SettingsFragment extends Fragment {
    private LinearLayout mPreferredQualityLayout;
    private LinearLayout mAutoplayLayout;
    private LinearLayout mMobileLayout;
    private LinearLayout mChatThemeLayout;
    private LinearLayout mUsernameLayout;
    private LinearLayout mLoginStatusLayout;
    private LinearLayout mThumbnailQualityLayout;
    private View mChatStartSetting;
    private Switch mChatStartSwitch;
    private TextView mQualityText, mPreferredQualityText, mLoginStatusText, mUsernameText, mThumbnailQualityText,
            mDefaultHomeText, mAutoplayText, mMobileVideoText, mChatThemeText, mVersionNumberText, mBuildNumberText,
            mChatSizeText;
    private EditText mUsernameEditText;
    private SharedPreferences mPreferences;
    private int mQualityTypeSelected, mPreferredQualitySelected, mAutoPlaySelected, mMobileLimitSelected,
            mBitmapQualitySelected, mDefaultHomeSelected, mChatThemeSelected, mChatSizeSelected;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_settings, container, false);
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null)
            actionBar.setTitle(getActivity().getString(R.string.title_settings));

        if (getActivity() == null) return rootView;

        Resources res = getActivity().getResources();

        // Video Settings
        LinearLayout qualityLayout = (LinearLayout) rootView.findViewById(R.id.qualitySetting);
        mQualityText = (TextView) qualityLayout.findViewById(R.id.textQualitySetting);
        mPreferredQualityLayout = (LinearLayout) rootView.findViewById(R.id.preferredQualitySetting);
        mPreferredQualityText = (TextView) mPreferredQualityLayout.findViewById(R.id.textPreferredQualitySetting);
        mAutoplayLayout = (LinearLayout) rootView.findViewById(R.id.videoAutoplaySetting);
        mAutoplayText = (TextView) mAutoplayLayout.findViewById(R.id.textAutoplaySetting);
        mMobileLayout = (LinearLayout) rootView.findViewById(R.id.videoMobileSetting);
        mMobileVideoText = (TextView) mMobileLayout.findViewById(R.id.textMobileSetting);

        // Twitch Settings
        mLoginStatusLayout = (LinearLayout) rootView.findViewById(R.id.settingsLoginStatus);
        mLoginStatusText = (TextView) mLoginStatusLayout.findViewById(R.id.textLoginStatus);
        mUsernameLayout = (LinearLayout) rootView.findViewById(R.id.usernameSetting);
        mUsernameText = (TextView) mUsernameLayout.findViewById(R.id.textUsernameSetting);
        LinearLayout twitchLoginLayout = (LinearLayout) rootView.findViewById(R.id.twitchLoginSetting);
        LinearLayout refreshTokenLayout = (LinearLayout) rootView.findViewById(R.id.refreshTokenSetting);

        // UI Settings
        mChatStartSetting = rootView.findViewById(R.id.chatStartSetting);
        mChatStartSwitch = (Switch) mChatStartSetting.findViewById(R.id.settingsChatStartSwitch);
        mChatThemeLayout = (LinearLayout) rootView.findViewById(R.id.chatThemeSetting);
        mChatThemeText = (TextView) mChatThemeLayout.findViewById(R.id.textChatTheme);
        LinearLayout chatSizeLayout = (LinearLayout) rootView.findViewById(R.id.chatSizeSetting);
        mChatSizeText = (TextView) chatSizeLayout.findViewById(R.id.textChatSize);
        mThumbnailQualityLayout = (LinearLayout) rootView.findViewById(R.id.thumbnailSetting);
        mThumbnailQualityText = (TextView) mThumbnailQualityLayout.findViewById(R.id.textThumbnailQuality);
        LinearLayout defaultHomeLayout = (LinearLayout) rootView.findViewById(R.id.defaultHomeSetting);
        mDefaultHomeText = (TextView) defaultHomeLayout.findViewById(R.id.textDefaultHome);

        // Ace Information
        mVersionNumberText = (TextView) rootView.findViewById(R.id.textAceVersion);
        mBuildNumberText = (TextView) rootView.findViewById(R.id.textAceBuildNumber);

        // Set initial values
        mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        mQualityTypeSelected = getTypeIndex(mPreferences.getString(Preferences.TWITCH_STREAM_QUALITY_TYPE, ""));
        mPreferredQualitySelected = getPrefQualityIndex(mPreferences.getString(Preferences.TWITCH_PREFERRED_VIDEO_QUALITY, ""));
        mMobileLimitSelected = getIndex(res.getStringArray(R.array.mobile_limit_settings), mPreferences.getString(Preferences.TWITCH_MOBILE_LIMIT, getString(R.string.mobile_limit_default)));
        mAutoPlaySelected = getAutoplayIndex(mPreferences.getString(Preferences.TWITCH_AUTOPLAY_MODE, getString(R.string.autoplay_default)));
        mBitmapQualitySelected = getBitmapQualityIndex(mPreferences.getString(Preferences.TWITCH_BITMAP_QUALITY, ""));
        mDefaultHomeSelected = mPreferences.getInt(Preferences.APP_DEFAULT_HOME, 0);
        mChatThemeSelected = getIndex(res.getStringArray(R.array.settings_chat_themes), mPreferences.getString(Preferences.CHAT_THEME, getString(R.string.settings_chat_theme_default)));
        mChatSizeSelected = mPreferences.getInt(Preferences.CHAT_SIZE, 18);

        updateTextFields();

        // Set Listeners
        qualityLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showQualityDialog();
            }
        });

        mPreferredQualityLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPreferredQualityDialog();
            }
        });

        mAutoplayLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAutoPlayDialog();
            }
        });

        mMobileLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMobileLimitDialog();
            }
        });

        if (mQualityTypeSelected == 1) disablePrefView();
        else enablePrefView();

        mUsernameLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showUsernameDialog();
            }
        });

        twitchLoginLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPreferences.getBoolean(Preferences.USER_HAS_TWITCH_USERNAME, false)) {
                    newLoginDialog();
                } else {
                    FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    transaction.replace(R.id.container, new SetupPagerFragment(), "setup");
                    transaction.commit();
                }
            }
        });

        refreshTokenLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AuthFragment a = new AuthFragment();
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.container, a);
                transaction.addToBackStack("auth");
                transaction.commit();
            }
        });

        twitchLoginLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPreferences.getBoolean(Preferences.USER_HAS_TWITCH_USERNAME, false)) {
                    newLoginDialog();
                } else {
                    FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    transaction.replace(R.id.container, new SetupPagerFragment(), "setup");
                    transaction.commit();
                }
            }
        });

        mThumbnailQualityLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBitmapDialog();
            }
        });

        mLoginStatusLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mPreferences.getBoolean(Preferences.USER_IS_AUTHENTICATED, false)) {
                    newLoginDialog();
                }
            }
        });

        defaultHomeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDefaultHomeDialog();
            }
        });

        mChatThemeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showChatThemeDialog();
            }
        });

        mChatStartSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mChatStartSwitch.setChecked(!mChatStartSwitch.isChecked());
                mChatStartSwitch.callOnClick();
            }
        });

        mChatStartSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPreferences.edit().putBoolean(Preferences.CHAT_AUTOSTART, mChatStartSwitch.isChecked()).apply();
            }
        });

        chatSizeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showChatSizeDialog();
            }
        });

        return rootView;
    }

    private void showQualityDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String types[] = getActivity().getResources().getStringArray(R.array.settings_stream_quality_type);


        builder.setTitle("Select Play Behaviour")
                .setSingleChoiceItems(types, mQualityTypeSelected, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mQualityTypeSelected = which;
                    }
                })
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mQualityText.setText(types[mQualityTypeSelected]);
                        mPreferences.edit().putString(Preferences.TWITCH_STREAM_QUALITY_TYPE, types[mQualityTypeSelected]).apply();
                        if (types[mQualityTypeSelected].equals("auto select best")) {
                            disablePrefView();
                        }
                        else {
                            enablePrefView();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        builder.create();
        builder.show();

        mThumbnailQualityLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBitmapDialog();
            }
        });

        mChatThemeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showChatThemeDialog();
            }
        });
    }

    private void showPreferredQualityDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String qualities[] = getActivity().getResources().getStringArray(R.array.livestream_qualities);

        builder.setTitle("Select Preferred Quality")
                .setSingleChoiceItems(qualities, mPreferredQualitySelected, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mPreferredQualitySelected = which;
                    }
                })
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mPreferredQualityText.setText(qualities[mPreferredQualitySelected]);
                        mPreferences.edit().putString(Preferences.TWITCH_PREFERRED_VIDEO_QUALITY, qualities[mPreferredQualitySelected]).apply();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        builder.create();
        builder.show();
    }

    private void showAutoPlayDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String autoSettings[] = getActivity().getResources().getStringArray(R.array.autoplay_settings);

        builder.setTitle("Select Autoplay Behaviour")
                .setSingleChoiceItems(autoSettings, mAutoPlaySelected, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mAutoPlaySelected = which;
                    }
                })
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mAutoplayText.setText(autoSettings[mAutoPlaySelected]);
                        mPreferences.edit().putString(Preferences.TWITCH_AUTOPLAY_MODE, autoSettings[mAutoPlaySelected]).apply();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        builder.create();
        builder.show();
    }

    private void showMobileLimitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String mobileSettings[] = getActivity().getResources().getStringArray(R.array.mobile_limit_settings);

        builder.setTitle("Select Mobile Limit")
                .setSingleChoiceItems(mobileSettings, mMobileLimitSelected, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mMobileLimitSelected = which;
                    }
                })
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mMobileVideoText.setText(mobileSettings[mMobileLimitSelected]);
                        mPreferences.edit().putString(Preferences.TWITCH_MOBILE_LIMIT, mobileSettings[mMobileLimitSelected]).apply();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        builder.create();
        builder.show();
    }

    private void showUsernameDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final AlertDialog alert;
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View mUsernameDialogView = inflater.inflate(R.layout.setting_username_dialog, null);
        mUsernameEditText = (EditText) mUsernameDialogView.findViewById(R.id.usernameEditText);

        builder.setTitle("Please Enter Username")
                .setView(mUsernameDialogView)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String request = getActivity().getResources().getString(R.string.twitch_user_url)
                                + mUsernameEditText.getText().toString();
                        mUsernameText.setText(mUsernameEditText.getText());
                        new DownloadJSONTask().execute(request);
                        mUsernameLayout.findViewById(R.id.usernameUpdateProgress).setVisibility(View.VISIBLE);
                        mUsernameLayout.findViewById(R.id.usernameStatusIcon).setVisibility(View.GONE);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        alert = builder.create();
        alert.show();

        mUsernameEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(
                            Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mUsernameEditText.getWindowToken(), 0);

                    String request = getActivity().getResources().getString(R.string.twitch_user_url)
                            + mUsernameEditText.getText().toString();
                    mUsernameText.setText(mUsernameEditText.getText());
                    new DownloadJSONTask().execute(request);
                    mUsernameLayout.findViewById(R.id.usernameUpdateProgress).setVisibility(View.VISIBLE);
                    mUsernameLayout.findViewById(R.id.usernameStatusIcon).setVisibility(View.GONE);
                    alert.cancel();
                    return true;
                }
                return false;
            }
        });
    }

    private void newLoginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle("New Twitch Account")
                .setMessage("Do you really want to log in with a new Account?")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
//                        CookieManager cookieManager = CookieManager.getInstance();
//                        CookieManager.getInstance().removeAllCookie();

                        FragmentTransaction transaction = getFragmentManager().beginTransaction();
                        transaction.replace(R.id.container, new SetupPagerFragment(), "setup");
                        transaction.commit();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        builder.create();
        builder.show();
    }

    private void newUserDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle("Recognized new Twitch Account")
                .setMessage("Do you want to log into Twitch for restricted content?")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        AuthFragment a = new AuthFragment();
                        FragmentTransaction transaction = getFragmentManager().beginTransaction();
                        transaction.replace(R.id.container, a);
                        transaction.addToBackStack("auth");
                        transaction.commit();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        builder.create();
        builder.show();
    }

    private void usernameDataReceived(JSONObject userData) {
        mUsernameLayout.findViewById(R.id.usernameUpdateProgress).setVisibility(View.GONE);
        String username, userDisplayName;
        try {
            username = userData.getString("name");
            userDisplayName = userData.getString("display_name");
            usernameConfirmed(username, userDisplayName);
            return;
        } catch (JSONException | NullPointerException e) {
            mUsernameLayout.findViewById(R.id.usernameStatusIcon).setVisibility(View.VISIBLE);
            ((ImageView)mUsernameLayout.findViewById(R.id.usernameStatusIcon)).setImageResource(R.drawable.ic_username_fail);
            mPreferences.edit().putBoolean(Preferences.USER_HAS_TWITCH_USERNAME, false).apply();
            mPreferences.edit().putBoolean(Preferences.USER_IS_AUTHENTICATED, false).apply();
            updateTextFields();
        }
        mPreferences.edit().putBoolean(Preferences.USER_HAS_TWITCH_USERNAME, false).apply();
        mPreferences.edit().putBoolean(Preferences.USER_IS_AUTHENTICATED, false).apply();
        mPreferences.edit().putString(Preferences.TWITCH_USERNAME, "nonamenonamenonamenonamenonamenoname").apply();
        mPreferences.edit().putString(Preferences.TWITCH_DISPLAY_USERNAME, "nonamenonamenonamenonamenonamenoname").apply();
        mPreferences.edit().putInt(Preferences.USERNAME_WARNING, 0).apply();
        mPreferences.edit().putInt(Preferences.AUTH_WARNING, 0).apply();

    }

    private void usernameConfirmed(String username, String userDisplayName) {
        mUsernameLayout.findViewById(R.id.usernameStatusIcon).setVisibility(View.VISIBLE);
        ((ImageView)mUsernameLayout.findViewById(R.id.usernameStatusIcon)).setImageResource(R.drawable.ic_login_auth);

        if (!username.equals(mPreferences.getString(Preferences.TWITCH_USERNAME, "asdfasgdasdgsgdsdfasfasdgasdf"))) {
            mPreferences.edit().putBoolean(Preferences.USER_HAS_TWITCH_USERNAME, true).apply();
            mPreferences.edit().putString(Preferences.TWITCH_USERNAME, username).apply();
            mPreferences.edit().putString(Preferences.TWITCH_DISPLAY_USERNAME, userDisplayName).apply();
            mPreferences.edit().putInt(Preferences.USERNAME_WARNING, 0).apply();
            mPreferences.edit().putInt(Preferences.AUTH_WARNING, 0).apply();

            mPreferences.edit().putBoolean(Preferences.USER_IS_AUTHENTICATED, false).apply();

            CookieManager cm = CookieManager.getInstance();
            if(android.os.Build.VERSION.SDK_INT >= 21){
                cm.removeAllCookies(null);
            }else{
                cm.removeAllCookie();
            }
            newUserDialog();
            updateTextFields();
        }
    }

    private void showBitmapDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String sizes[] = getActivity().getResources().getStringArray(R.array.settings_bitmap_qualities);

        builder.setTitle("Select Preferred Quality")
                .setSingleChoiceItems(sizes, mBitmapQualitySelected, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mBitmapQualitySelected = which;
                    }
                })
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mThumbnailQualityText.setText(sizes[mBitmapQualitySelected]);
                        mPreferences.edit().putString(Preferences.TWITCH_BITMAP_QUALITY, sizes[mBitmapQualitySelected]).apply();
                        ((MainActivity)getActivity()).setBitmapQuality();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        builder.create();
        builder.show();
    }

    private void showDefaultHomeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String labels[] = getActivity().getResources().getStringArray(R.array.drawer_labels);

        builder.setTitle("Select Default Homescreen")
                .setSingleChoiceItems(labels, mDefaultHomeSelected, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDefaultHomeSelected = which;
                    }
                })
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (!mPreferences.getBoolean(Preferences.USER_HAS_TWITCH_USERNAME, false) && mDefaultHomeSelected == 3) {
                            Preferences.showUsernameToast(getActivity());
                            return;
                        }
                        mDefaultHomeText.setText(labels[mDefaultHomeSelected]);
                        mPreferences.edit().putInt(Preferences.APP_DEFAULT_HOME, mDefaultHomeSelected).apply();

                        FragmentManager fm = getFragmentManager();
                        fm.popBackStack(fm.getBackStackEntryAt(0).getName(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
                        ((MainActivity)getActivity()).gotoHome();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        builder.create();
        builder.show();
    }

    private void showChatThemeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String themes[] = getActivity().getResources().getStringArray(R.array.settings_chat_themes);

        builder.setTitle("Chat Theme")
                .setSingleChoiceItems(themes, mChatThemeSelected, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mChatThemeSelected = which;
                    }
                })
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mChatThemeText.setText(themes[mChatThemeSelected]);
                        mPreferences.edit().putString(Preferences.CHAT_THEME, themes[mChatThemeSelected]).apply();
                        ((MainActivity)getActivity()).setBitmapQuality();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        builder.create();
        builder.show();
    }

    private void showChatSizeDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final AlertDialog alert;
        final int offset = 8;

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View chatSizeView = inflater.inflate(R.layout.setting_chatsize_dialog, null);
        final NumberPicker np = (NumberPicker) chatSizeView.findViewById(R.id.np);

        String[] nums = new String[25];
        for(int i=0; i < nums.length; i++) {
            nums[i] = Integer.toString(i+offset);
            if (i+offset == 18)
                nums[i] = Integer.toString(i+offset) + " (default)";
        }
        np.setMinValue(0);
        np.setMaxValue(nums.length-1);
        np.setDisplayedValues(nums);
        np.setValue(mChatSizeSelected-offset);
        np.setWrapSelectorWheel(false);
        np.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        builder.setTitle("Choose Textsize")
                .setView(chatSizeView)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        np.clearFocus();
                        mChatSizeSelected = np.getValue() + offset;
                        mPreferences.edit().putInt(Preferences.CHAT_SIZE, mChatSizeSelected).apply();
                        updateTextFields();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        alert = builder.create();
        alert.show();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onResume(){
        super.onResume();
        ((MainActivity)getActivity()).pauseAd();
        AceAnims.showActionbar(getActivity(), false);
    }

    @Override
    public void onPause(){
        super.onPause();
        ((MainActivity)getActivity()).resumeAd();
    }

    private void updateTextFields() {
        mQualityText.setText(mPreferences.getString(Preferences.TWITCH_STREAM_QUALITY_TYPE, ""));
        mPreferredQualityText.setText(mPreferences.getString(Preferences.TWITCH_PREFERRED_VIDEO_QUALITY, ""));
        mAutoplayText.setText(mPreferences.getString(Preferences.TWITCH_AUTOPLAY_MODE, getString(R.string.autoplay_default)));
        mMobileVideoText.setText(mPreferences.getString(Preferences.TWITCH_MOBILE_LIMIT, getString(R.string.mobile_limit_default)));

        mThumbnailQualityText.setText(mPreferences.getString(Preferences.TWITCH_BITMAP_QUALITY, ""));
        mChatThemeText.setText(mPreferences.getString(Preferences.CHAT_THEME, getString(R.string.settings_chat_theme_default)));

        mChatSizeText.setText("" + mChatSizeSelected);
        if (mChatSizeSelected == 18)
            mChatSizeText.setText(mChatSizeSelected + " (default)");


        if (mVersionNumberText != null) {
            try {
                String versionName = getActivity().getPackageManager()
                        .getPackageInfo(getActivity().getPackageName(), 0).versionName;
                mVersionNumberText.setText(versionName);
            } catch (PackageManager.NameNotFoundException e) {
                Log.d("version", "Could not load Version");
            }
        }

        if (mBuildNumberText != null) {
            try {
                String versionCode = "" + getActivity().getPackageManager()
                        .getPackageInfo(getActivity().getPackageName(), 0).versionCode;
                mBuildNumberText.setText(versionCode);
            } catch (PackageManager.NameNotFoundException e) {
                Log.d("version", "Could not load Buildnumber");
            }
        }

        if (mPreferences.getBoolean(Preferences.USER_IS_AUTHENTICATED, false)) {
            mLoginStatusText.setText("Logged In and Authorized");
            mUsernameText.setText(mPreferences.getString(Preferences.TWITCH_DISPLAY_USERNAME, ""));
            ((ImageView)mLoginStatusLayout.findViewById(R.id.loginStatusIcon)).setImageResource(R.drawable.ic_login_auth);
        }
        else if (mPreferences.getBoolean(Preferences.USER_HAS_TWITCH_USERNAME, false)) {
            mLoginStatusText.setText("Logged In with Username, not yet Authorized");
            mUsernameText.setText(mPreferences.getString(Preferences.TWITCH_DISPLAY_USERNAME, ""));
            ((ImageView)mLoginStatusLayout.findViewById(R.id.loginStatusIcon)).setImageResource(R.drawable.ic_login_user);
        }
        else {
            mUsernameText.setText("No Username set");
            mLoginStatusText.setText("Not logged in");
            ((ImageView)mLoginStatusLayout.findViewById(R.id.loginStatusIcon)).setImageResource(R.drawable.ic_username_fail);
        }

        if (mPreferences.getBoolean(Preferences.USER_HAS_TWITCH_USERNAME, false))
            ((ImageView)mUsernameLayout.findViewById(R.id.usernameStatusIcon)).setImageResource(R.drawable.ic_login_auth);
        else
            ((ImageView)mUsernameLayout.findViewById(R.id.usernameStatusIcon)).setImageResource(R.drawable.ic_username_fail);

        mDefaultHomeText.setText(getHomeText(mDefaultHomeSelected));

    }

    private int getIndex(String settings[], String s) {
        for (int i = 0; i < settings.length; i++) {
            if (s.equals(settings[i])) return i;
        }
        return 0;
    }

    private int getTypeIndex(String s) {
        String types[] = getActivity().getResources().getStringArray(R.array.settings_stream_quality_type);
        for (int i = 0; i < types.length; i++) {
            if (s.equals(types[i])) return i;
        }
        return 0;
    }

    private int getPrefQualityIndex(String s) {
        String types[] = getActivity().getResources().getStringArray(R.array.livestream_qualities);
        for (int i = 0; i < types.length; i++) {
            if (s.equals(types[i])) return i;
        }
        return 0;
    }

    private int getAutoplayIndex(String s) {
        String types[] = getActivity().getResources().getStringArray(R.array.autoplay_settings);
        for (int i = 0; i < types.length; i++) {
            if (s.equals(types[i])) return i;
        }
        return 0;
    }

    private int getBitmapQualityIndex(String s) {
        String sizes[] = getActivity().getResources().getStringArray(R.array.settings_bitmap_qualities);
        for (int i = 0; i < sizes.length; i++) {
            if (s.equals(sizes[i])) return i;
        }
        return 0;
    }

    private String getHomeText(int i) {
        String labels[] = getActivity().getResources().getStringArray(R.array.drawer_labels);
        if (i < labels.length && i >= 0)
            return labels[i];
        return labels[0];
    }

    private class DownloadJSONTask extends AsyncTask<String, Void, JSONObject> {

        public DownloadJSONTask(){}

        protected JSONObject doInBackground(String... urls) {
            return TwitchNetworkTasks.downloadJSONData(urls[0]);
        }

        protected void onPostExecute(JSONObject result) {
            usernameDataReceived(result);
        }
    }

    private void disablePrefView () {
        final int disabled = getActivity().getResources().getColor(R.color.primary_text_disabled_material_light);
        mPreferredQualityText.setTextColor(disabled);
        mPreferredQualityLayout.setClickable(false);
    }

    private void enablePrefView () {
        final int enabled = getActivity().getResources().getColor(R.color.secondary_text_default_material_light);
        mPreferredQualityText.setTextColor(enabled);
        mPreferredQualityLayout.setClickable(true);
    }
}