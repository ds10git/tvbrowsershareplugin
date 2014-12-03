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

import java.util.Date;
import java.util.List;

import org.tvbrowser.devplugin.Channel;
import org.tvbrowser.devplugin.Plugin;
import org.tvbrowser.devplugin.PluginMenu;
import org.tvbrowser.devplugin.Program;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;

/**
 * A service class that provides a share functionality for TV-Browser for Android.
 * 
 * @author René Mach
 */
public class TVBrowserSharePlugin extends Service {
  /* The id for the share PluginMenu */
  private static final int SHARE_MENU_ID = 1;
  
  /* The version of this Plugin */
  private static final String VERSION = "0.1";
  
  @Override
  public IBinder onBind(Intent intent) {
    return getBinder;
  }
  
  private final Plugin.Stub getBinder = new Plugin.Stub() {
    @Override
    public String getVersion() throws RemoteException {
      return VERSION;
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
      return null;
    }
    
    @Override
    public boolean onProgramContextMenuSelected(Program program, PluginMenu pluginMenu) throws RemoteException {
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
      sendIntent.putExtra(Intent.EXTRA_TEXT, message.toString());
      sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject.toString());
      
      if(mailto != null) {
        sendIntent.putExtra(Intent.EXTRA_EMAIL, mailto);
      }
      
      sendIntent.setType("text/plain");
      
      Intent chooser = Intent.createChooser(sendIntent, getString(R.string.service_share_context_menu));
      chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      
      startActivity(chooser);
      
      // Don't mark the program.
      return false;
    }
    
    @Override
    public PluginMenu[] getContextMenuActionsForProgram(Program program) throws RemoteException {
      PluginMenu share = new PluginMenu(SHARE_MENU_ID, getString(R.string.service_share_context_menu));
      
      return new PluginMenu[] {share};
    }

    @Override
    public boolean hasPreferences() throws RemoteException {
      return true;
    }

    @Override
    public void openPreferences(List<Channel> subscribedChannels) throws RemoteException {
      Intent startPref = new Intent(TVBrowserSharePlugin.this, TVBSharePluginPreferencesActivity.class);
      startPref.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(startPref);
    }

    @Override
    public long[] getMarkedPrograms() throws RemoteException {
      return null;
    }

    @Override
    public void handleFirstKnownProgramId(long programId) throws RemoteException {}

    @Override
    public void onActivation() throws RemoteException {}

    @Override
    public void onDeactivation() throws RemoteException {}
  };
}
