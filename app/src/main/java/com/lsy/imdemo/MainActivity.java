package com.lsy.imdemo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lsy.imdemo.bean.Data;
import com.lsy.imdemo.bean.MsgBean;
import com.lsy.imdemo.bean.UserBean;
import com.zhy.view.flowlayout.FlowLayout;
import com.zhy.view.flowlayout.TagAdapter;
import com.zhy.view.flowlayout.TagFlowLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class MainActivity extends BaseActivity {

    private Button start;
    private TextView text, tv_name;
    private EditText edit_insert;
    private Button tv_enter, btn_clear;
    private WebSocket mWebSocket;
    private TagFlowLayout mFlowLayout;
    private TagAdapter mAdapter;
    private List<UserBean> list = new ArrayList<>();
    private LayoutInflater mInflater;
    private String currentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mInflater = LayoutInflater.from(this);

        start = findViewById(R.id.start);
        text = findViewById(R.id.text);
        tv_name = findViewById(R.id.tv_name);
        edit_insert = findViewById(R.id.edit_insert);
        tv_enter = findViewById(R.id.tv_enter);
        btn_clear = findViewById(R.id.btn_clear);
        mFlowLayout = findViewById(R.id.id_flowlayout);
        tv_enter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onEnter();
            }
        });

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = StrUtil.getRandomName();
                tv_name.setText(String.format("当前用户:%s", name));
                currentId = StrUtil.getRandomId();
                connect(name, currentId);
            }
        });

        btn_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                text.setText("");
            }
        });

        mAdapter = new TagAdapter<UserBean>(list) {

            @Override
            public View getView(FlowLayout parent, int position, UserBean userBean) {
                TextView tv = (TextView) mInflater.inflate(R.layout.tv,
                        mFlowLayout, false);
                tv.setText(userBean.name);
                return tv;
            }
        };

        mFlowLayout.setAdapter(mAdapter);

    }

    @Override
    protected void onDestroy() {
        if (mWebSocket != null) {
            mWebSocket.close(50, "关闭");
        }
        super.onDestroy();
    }

    /**
     * 发送消息
     */
    private void onEnter() {
        if (mWebSocket != null) {
            String content = edit_insert.getText().toString();
            if (TextUtils.isEmpty(content)) {
                showToast("发送内容不能为空");
                return;
            }
            Set<Integer> select = mFlowLayout.getSelectedList();

            MsgBean msgBean = new MsgBean();
            msgBean.content = content;
            if (select.iterator().hasNext()) {
                UserBean userBean = list.get(select.iterator().next());
                msgBean.toid = userBean.id;
                msgBean.toname = userBean.name;
                if (userBean.id.equals(currentId)) {
                    output(String.format("你对自己说：%s", msgBean.content));
                    return;
                } else {
                    output(String.format("你对%s 说：%s", userBean.name, msgBean.content));
                }
            } else {
                output(String.format("你对大家说：%s", msgBean.content));
            }
            boolean isSend = mWebSocket.send(new Gson().toJson(msgBean));
            if (isSend) {
                edit_insert.setText("");
            } else {
                showToast("发送失败，链接失效");
            }
        } else {
            showToast("链接未初始化");
        }
    }

    private void connect(String name, String id) {

        EchoWebSocketListener listener = new EchoWebSocketListener();
        Request request = new Request.Builder()
                .url(BuildConfig.IS_ONLINE ? BuildConfig.ONLINE_URL : BuildConfig.LOCAL_URL)
                .addHeader("name", name)
                .addHeader("id", id)
                .build();
        OkHttpClient client = new OkHttpClient();
        client.newWebSocket(request, listener);

//        client.dispatcher().executorService().shutdown();
    }

    private final class EchoWebSocketListener extends WebSocketListener {

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            MainActivity.this.mWebSocket = webSocket;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    start.setEnabled(false);
                }
            });
        }

        @Override
        public void onMessage(WebSocket webSocket, final String text) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    manageData(text);
                }
            });
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            output("onMessage byteString: " + bytes);
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            output("onClosing: " + code + "/" + reason);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            output("onClosed: " + code + "/" + reason);
            mWebSocket = null;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    start.setEnabled(true);
                }
            });
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            output("onFailure: " + t.getMessage());
            if (mWebSocket != null) {
                mWebSocket.close(1000, "再见");
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    start.setEnabled(true);
                }
            });
        }
    }

    /**
     * 处理数据
     *
     * @param text
     */
    private void manageData(String text) {
        Data data = new Gson().fromJson(text, Data.class);
        if (data != null && data.datacontent != null) {
            String content = data.datacontent.toString();
            switch (data.datatype) {
                case 1:
                    MsgBean msgBean = new Gson().fromJson(content, MsgBean.class);
                    if (TextUtils.isEmpty(msgBean.toid)) {
                        if (msgBean.fromid.equals(currentId)) {
                            return;
                        } else {
                            output(String.format("%s 对大家说：%s", msgBean.fromname, msgBean.content));
                        }
                    } else {
                        output(String.format("%s 对你说：%s", msgBean.fromname, msgBean.content));
                    }
                    break;
                case 2:
                    List<UserBean> listData = new Gson().fromJson(content, new TypeToken<List<UserBean>>() {
                    }.getType());
                    list.clear();
                    list.addAll(listData);
                    mAdapter.notifyDataChanged();
                    break;
                default:
            }
        }
    }


    private void output(final String content) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text.setText(text.getText().toString() + content + "\n");
            }
        });
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("确认退出?")
                .setPositiveButton("退出", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).show();
    }
}
