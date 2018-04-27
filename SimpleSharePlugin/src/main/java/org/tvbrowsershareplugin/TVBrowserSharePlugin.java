/*
 * TVBrowserSharePlugin for TV-Browser for Android
 * Copyright (C) 2014 René Mach (rene@tvbrowser.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to use, copy, modify or merge the Software,
 * furthermore to publish and distribute the Software free of charge without modifications and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.tvbrowsershareplugin;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.tvbrowser.devplugin.Channel;
import org.tvbrowser.devplugin.Plugin;
import org.tvbrowser.devplugin.PluginManager;
import org.tvbrowser.devplugin.PluginMenu;
import org.tvbrowser.devplugin.Program;
import org.tvbrowser.devplugin.ReceiveTarget;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.DateFormat;

/**
 * A service class that provides a share functionality for TV-Browser for Android.
 * 
 * @author René Mach
 */
public class TVBrowserSharePlugin extends Service {
  public static final String PREF_MARKINGS = "PREF_MARKINGS";
  /* The id for the share PluginMenu */
  private static final int SHARE_MENU_ID = 1;
  
  /* The id for the share only title PluginMenu */
  private static final int SHARE_ONLY_TITLE_MENU_ID = 2;    
  
  /* The id for the remove marking PluginMenu */
  private static final int SHARE_REMOVE_MARKING_ID = 3;   
  
  /* The plugin manager of TV-Browser */
  private PluginManager mPluginManager;
  
  /* The set with the marking ids */
  private Set<String> mMarkingProgramIds;
    
  @Override
  public IBinder onBind(Intent intent) {
    return getBinder;
  }
  
  @Override
  public boolean onUnbind(Intent intent) {
    /* Don't keep instance of plugin manager*/
    mPluginManager = null;
    
    stopSelf();
    
    return false;
  }
  
  @Override
  public void onDestroy() {
    /* Don't keep instance of plugin manager*/
    mPluginManager = null;
    
    super.onDestroy();
  }
  
  private void save() {
    Editor edit = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
    edit.remove(PREF_MARKINGS);
    edit.putString(PREF_MARKINGS, TextUtils.join(";", mMarkingProgramIds));
    edit.commit();
  }
  
  private final Plugin.Stub getBinder = new Plugin.Stub() {
    private long mRemovingProgramId = -1;
    
    @Override
    public String getVersion() throws RemoteException {
      String version = "UNKONW";

      try {
        PackageInfo pInfo = getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0);
        version = pInfo.versionName;
      } catch (PackageManager.NameNotFoundException e) {
        e.printStackTrace();
      }
      return version;
    }

    @Override
    public String getDescription() throws RemoteException {
      return getString(R.string.service_share_description);
    }

    @Override
    public String getAuthor() throws RemoteException {
      return "René Mach";
    }

    @Override
    public String getLicense() throws RemoteException {
      return getString(R.string.license);
    }
    
    @Override
    public String getName() throws RemoteException {
      return getString(R.string.service_share_name);
    }
    
    @Override
    public byte[] getMarkIcon() throws RemoteException {
      Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_share);
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      icon.compress(Bitmap.CompressFormat.PNG, 100, stream);
      
      return stream.toByteArray();
    }
    
    @Override
    public boolean onProgramContextMenuSelected(Program program, PluginMenu pluginMenu) throws RemoteException {
      boolean result = false;
      
      if(pluginMenu.getId() == SHARE_REMOVE_MARKING_ID) {
        if(mMarkingProgramIds.contains(String.valueOf(program.getId()))) {
          mRemovingProgramId = program.getId();
          
          boolean unmarked = false;
          
          if(mPluginManager.getTvBrowserSettings().getTvbVersionCode() >= 308) {
            unmarked = mPluginManager.unmarkProgramWithIcon(program, TVBrowserSharePlugin.class.getCanonicalName());
          }
          else {
            unmarked = mPluginManager.unmarkProgram(program);
          }
          
          if(unmarked) {
            mMarkingProgramIds.remove(String.valueOf(program.getId()));
            save();
          }
          
          mRemovingProgramId = -1;
        }
      }
      else {
        StringBuilder message = new StringBuilder();
        StringBuilder subject = new StringBuilder();
        
        String startDate = DateFormat.getLongDateFormat(TVBrowserSharePlugin.this).format(new Date(program.getStartTimeInUTC()));
        String startTime = DateFormat.getTimeFormat(TVBrowserSharePlugin.this).format(new Date(program.getStartTimeInUTC()));
        String endTime = DateFormat.getTimeFormat(TVBrowserSharePlugin.this).format(new Date(program.getEndTimeInUTC()));
        
        subject.append(startDate).append(", ").append(startTime).append(" - ").append(endTime).append(" ").append(program.getChannel().getChannelName()).append(": ");
        subject.append(program.getTitle());
        
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(TVBrowserSharePlugin.this);
        
        if(pref.getBoolean(getString(R.string.PREF_SHARE_MESSAGE_CONTAINS_DATE), getResources().getBoolean(R.bool.pref_share_message_contains_date_default))) {
          message.append(startDate).append(", ").append(startTime).append(" - ").append(endTime);
        }
  
        if(pref.getBoolean(getString(R.string.PREF_SHARE_MESSAGE_CONTAINS_CHANNEL), getResources().getBoolean(R.bool.pref_share_message_contains_channel_default))) {
          if(message.toString().trim().length() > 0) {
            message.append(" ");
          }
          
          message.append(program.getChannel().getChannelName()).append(": ");
        }
        
        if(message.toString().trim().length() > 0) {
          message.append("\n\n");
        }
        
        message.append(program.getTitle());
        
        String desc = null;
        
        if(pref.getString(getString(R.string.PREF_SHARE_DESCRIPTION_TYPE), getString(R.string.pref_share_description_type_default)).equals("0")) {
          desc = program.getShortDescription();
          
          if(desc == null || desc.trim().length() == 0) {
            desc = program.getDescription();
          }
          
          if(desc != null && desc.length() > 160) {
            desc = desc.substring(0,160) +"\u2026";
          }
        }
        else {
          desc = program.getDescription();
          
          if(desc == null) {
            desc = program.getShortDescription();
          }
        }
        
        if(program.getEpisodeTitle() != null) {
          subject.append(" - ").append(program.getEpisodeTitle());
          message.append(" - ").append(program.getEpisodeTitle());
        }
        
        if(desc != null) {
          message.append("\n\n").append(desc);
        }
        
        String[] mailto = null;
        
        String mailAddresses = pref.getString(getString(R.string.PREF_SHARE_TARGET_ADDRESS), "").trim();
        
        if(mailAddresses.contains(",")) {
          mailto = mailAddresses.split("\\s+,\\s+");
        }
        else if(mailAddresses.trim().length() > 0) {
          mailto = new String[1];
          mailto[0] = mailAddresses;
        }
        
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        
        if(pluginMenu.getId() == SHARE_ONLY_TITLE_MENU_ID) {
          subject.delete(0, subject.length());
          subject.append(program.getTitle());
          
          message.delete(0, message.length());
          message.append(program.getTitle());
        }
        
        sendIntent.putExtra(Intent.EXTRA_TEXT, message.toString());
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject.toString());
        
        if(mailto != null) {
          sendIntent.putExtra(Intent.EXTRA_EMAIL, mailto);
        }
        
        sendIntent.setType("text/plain");
        
        Intent chooser = Intent.createChooser(sendIntent, getString(R.string.service_share_context_menu));
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        startActivity(chooser);
        
        // mark the program if user has setup marking
        result = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(getString(R.string.PREF_SHARE_MARK_PROGRAMS), getResources().getBoolean(R.bool.pref_share_mark_programs_default));
        
        if(result && !mMarkingProgramIds.contains(String.valueOf(program.getId()))) {
          mMarkingProgramIds.add(String.valueOf(program.getId()));
          save();
        }
      }
      
      return result;
    }
    
    @Override
    public PluginMenu[] getContextMenuActionsForProgram(Program program) throws RemoteException {
      ArrayList<PluginMenu> menuList = new ArrayList<PluginMenu>();
      
      menuList.add(new PluginMenu(SHARE_MENU_ID, getString(R.string.service_share_context_menu)));
      
      if(PreferenceManager.getDefaultSharedPreferences(TVBrowserSharePlugin.this).getBoolean(getString(R.string.PREF_SHARE_ONLY_TITLE_AVAILABLE), getResources().getBoolean(R.bool.pref_share_only_title_available_default))) {
        menuList.add(new PluginMenu(SHARE_ONLY_TITLE_MENU_ID, getString(R.string.service_share_only_title_context_menu)));
      }
      
      if(mMarkingProgramIds.contains(String.valueOf(program.getId()))) {
        menuList.add(new PluginMenu(SHARE_REMOVE_MARKING_ID, getString(R.string.service_share_context_menu_remove_marking)));
      }
      
      return menuList.toArray(new PluginMenu[menuList.size()]);
    }

    @Override
    public boolean hasPreferences() throws RemoteException {
      return true;
    }

    @Override
    public void openPreferences(List<Channel> subscribedChannels) throws RemoteException {
      Intent startPref = new Intent(TVBrowserSharePlugin.this, TVBSharePluginPreferencesActivity.class);
      startPref.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      
      if(mPluginManager != null) {
        startPref.putExtra(TVBSharePluginPreferencesActivity.DARK_THEME_EXTRA_KEY, mPluginManager.getTvBrowserSettings().isUsingDarkTheme());
      }
      
      startActivity(startPref);
    }

    @Override
    public long[] getMarkedPrograms() throws RemoteException {
      long[] markings = new long[mMarkingProgramIds.size()];
      
      Iterator<String> values = mMarkingProgramIds.iterator();
      
      for(int i = 0; i < markings.length; i++) {
        markings[i] = Long.parseLong(values.next());
      }
      
      return markings;
    }

    @Override
    public void handleFirstKnownProgramId(long programId) throws RemoteException {
      if(programId == -1) {
        mMarkingProgramIds.clear();
      }
      else {
        String[] knownIds = mMarkingProgramIds.toArray(new String[mMarkingProgramIds.size()]);
        
        for(int i = knownIds.length-1; i >= 0; i--) {
          if(Long.parseLong(knownIds[i]) < programId) {
            mMarkingProgramIds.remove(knownIds[i]);
          }
        }
      }
    }

    @Override
    public void onActivation(PluginManager pluginManager) throws RemoteException {
      mPluginManager = pluginManager;
      
      mMarkingProgramIds = new HashSet<String>();
      String test = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(PREF_MARKINGS, null);
      
      if(test != null) {
        mMarkingProgramIds.addAll(Arrays.asList(((String)test).split(";")));
      }
    }

    @Override
    public void onDeactivation() throws RemoteException {
      /* Don't keep instance of plugin manager*/
      mPluginManager = null;
    }

    @Override
    public boolean isMarked(long programId) throws RemoteException {
      return programId != mRemovingProgramId && mMarkingProgramIds.contains(String.valueOf(programId));
    }

    @Override
    public ReceiveTarget[] getAvailableProgramReceiveTargets() throws RemoteException {
      return null;
    }

    @Override
    public void receivePrograms(Program[] programs, ReceiveTarget target) throws RemoteException {}
  };
}
