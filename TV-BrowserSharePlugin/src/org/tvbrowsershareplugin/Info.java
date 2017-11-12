package org.tvbrowsershareplugin;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

public class Info extends AppCompatActivity {
  @Override
  protected void onResume() {
    super.onResume();
    
    final AlertDialog.Builder b = new AlertDialog.Builder(Info.this);
    b.setCancelable(false);
    b.setTitle(R.string.app_name);
    b.setMessage(R.string.info);
    b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        finish();
      }
    });
    b.show();
  }
}
