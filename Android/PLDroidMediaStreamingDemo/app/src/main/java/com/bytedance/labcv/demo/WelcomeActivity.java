package com.bytedance.labcv.demo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bytedance.labcv.demo.base.BaseActivity;
import com.bytedance.labcv.demo.contract.WelcomeContract;
import com.bytedance.labcv.demo.contract.presenter.WelcomePresenter;
import com.bytedance.labcv.demo.utils.CommonUtils;
import com.qiniu.pili.droid.streaming.demo.MainActivity;
import com.qiniu.pili.droid.streaming.demo.R;
import com.qiniu.pili.droid.streaming.effect.PLVideoViewActivity;

import static com.qiniu.pili.droid.streaming.effect.utils.Config.ROOM_NUMBER;
import static com.qiniu.pili.droid.streaming.effect.utils.Utils.requestPlayUrl;

/**
 * 欢迎界面
 */
public class WelcomeActivity extends BaseActivity<WelcomeContract.Presenter>
        implements WelcomeContract.View {
    private TextView mTvVersion;
    private Button mBtStart;
    private Button mBtQiNiu;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        setPresenter(new WelcomePresenter());

        initView();

        if (!mPresenter.resourceReady()) {
            mPresenter.startTask();
        } else {
            mBtStart.setEnabled(true);
            mBtQiNiu.setEnabled(true);
        }
    }

    private void initView() {
        mTvVersion = (TextView) findViewById(R.id.tv_version);
        mBtStart = (Button) findViewById(R.id.bt_start);
        mProgressBar = (ProgressBar) findViewById(R.id.progress);
        mBtStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CommonUtils.isFastClick()) {
                    return;
                }
                Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
        // 增加隐藏功能：长按重新加载资源，避免更换资源后要重装应用
        // Long press to reload the resource
        // to avoid needing to reload the application after replacing the resource
        mBtStart.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mPresenter.startTask();
                return true;
            }
        });
        mTvVersion.setText(mPresenter.getVersionName());

        mBtQiNiu = (Button) findViewById(R.id.bt_qiniu);
        mBtQiNiu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (CommonUtils.isFastClick()) {
                    return;
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String playUrl = requestPlayUrl(ROOM_NUMBER);
                        Intent intent = new Intent(WelcomeActivity.this, PLVideoViewActivity.class);
                        intent.putExtra("videoPath", playUrl);
                        startActivity(intent);
                    }
                }).start();
            }
        });
    }

    @Override
    public void onStartTask() {
        mBtStart.setEnabled(false);
        mBtStart.setText(getString(R.string.resource_prepare));

        mProgressBar.setIndeterminate(true);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onEndTask(boolean result) {
        mProgressBar.setVisibility(View.GONE);
        if (result) {
            mBtStart.setText(getString(R.string.start));
            mBtStart.setEnabled(true);
            mBtQiNiu.setEnabled(true);
        } else {
            Toast.makeText(this, getString(R.string.resource_ready), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public Context getContext() {
        return getApplicationContext();
    }
}
