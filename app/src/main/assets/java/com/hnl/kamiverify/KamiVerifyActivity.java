package com.hnl.kamiverify;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class KamiVerifyActivity extends Activity {
    private EditText et;
    private ArrayList<String> list = new ArrayList<>();
    private String target = "TARGET_ACTIVITY_PLACEHOLDER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            InputStream is = getAssets().open("kami_list.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                list.add(line.trim());
            }
            br.close();
        } catch (Exception e) {
            // ignore
        }
        showDialog();
    }

    private void showDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("VERIFY_TITLE_PLACEHOLDER");
        b.setMessage("VERIFY_SUBTITLE_PLACEHOLDER");
        et = new EditText(this);
        et.setHint("请输入卡密");
        b.setView(et);
        b.setPositiveButton("验证", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String input = et.getText().toString().trim();
                if (list.contains(input)) {
                    launch();
                } else {
                    Toast.makeText(KamiVerifyActivity.this, "卡密错误，请重新输入", Toast.LENGTH_SHORT).show();
                    showDialog();
                }
            }
        });
        b.setNegativeButton("退出", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        b.setCancelable(false);
        b.create().show();
    }

    private void launch() {
        if (target != null && !target.isEmpty()) {
            Intent intent = new Intent();
            intent.setClassName(getPackageName(), target);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }
}
