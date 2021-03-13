package com.androidvip.hebf.activities.internal;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.androidvip.hebf.helpers.HebfCommandLine;
import com.androidvip.hebf.R;
import com.androidvip.hebf.utils.K;
import com.androidvip.hebf.utils.Themes;
import com.androidvip.hebf.utils.UserPrefs;
import com.androidvip.hebf.utils.Utils;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

public class CommandLineActivity extends AppCompatActivity {
    private EditText input;
    private String inputPlaceHolder = "user@hebf:/ $ ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_command_line);

        HebfCommandLine cmd = new HebfCommandLine(new WeakReference<>(this));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (new UserPrefs(getApplicationContext()).getString(K.PREF.THEME, Themes.LIGHT).equals(Themes.WHITE)) {
            toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.colorAccentWhite));
            toolbar.setSubtitleTextColor(ContextCompat.getColor(this, R.color.darkness));
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_theme);
        }

        input = findViewById(R.id.command_line_input);
        input.setSelection(input.getText().length());
        focusEditText(null);
        TextView resultText = findViewById(R.id.command_line_result);
        cmd.setOnCompleteListener((result, isSuccessful) -> {
            UserPrefs userPrefs = new UserPrefs(getApplicationContext());
            Set<String> achievementsSet = userPrefs.getStringSet(K.PREF.ACHIEVEMENT_SET, new HashSet<>());
            if (!achievementsSet.contains("command-line")) {
                Utils.addAchievement(getApplicationContext(), "command-line");
                Toast.makeText(this, getString(R.string.achievement_unlocked, getString(R.string.achievement_command_line)), Toast.LENGTH_LONG).show();
            }
            if (!isSuccessful) {
                resultText.setTextColor(Color.parseColor("#ff1744"));
            } else {
                resultText.setTextColor(Color.parseColor("#ffffff"));
            }
            resultText.setText(result);
            input.setSelection(input.getText().length());
        });

        cmd.setOnUserChangedListener(isSU -> {
            if (isSU) {
                Toast.makeText(this, "Working as superuser", Toast.LENGTH_SHORT).show();
                getSupportActionBar().setTitle("HEBF Terminal (superuser)");
                inputPlaceHolder = "root@hebf:/ # ";
            } else {
                getSupportActionBar().setTitle("HEBF Terminal");
                inputPlaceHolder = "user@hebf:/ $ ";
            }
            input.setText(inputPlaceHolder);
            input.setSelection(input.getText().length());
        });

        input.setOnKeyListener((v, keyCode, event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                String[] args = input.getText().toString().replace(inputPlaceHolder, "").trim().split(" ");
                cmd.run(args);
                focusEditText(v);
                return true;
            }
            return false;
        });

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {

            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            @Override
            public void afterTextChanged(Editable s) {
                if(!s.toString().startsWith(inputPlaceHolder)){
                    input.setText(inputPlaceHolder);
                    Selection.setSelection(input.getText(), input.getText().length());
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public void focusEditText(View view) {
        input.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }
    }
}
