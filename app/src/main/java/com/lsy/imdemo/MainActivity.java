package com.lsy.imdemo;

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
import java.util.HashSet;
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
    private TextView text;
    private EditText edit_insert;
    private Button tv_enter;
    private WebSocket webSocket;
    private TagFlowLayout mFlowLayout;
    private TagAdapter mAdapter;
    private List<UserBean> list = new ArrayList<>();
    private LayoutInflater mInflater;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mInflater = LayoutInflater.from(this);

        start = findViewById(R.id.start);
        text = findViewById(R.id.text);
        edit_insert = findViewById(R.id.edit_insert);
        tv_enter = findViewById(R.id.tv_enter);
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
                connect(StrUtil.getRandomName(), StrUtil.getRandomId());
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


    /**
     * 发送消息
     */
    private void onEnter() {
        if (webSocket != null) {
            String content = edit_insert.getText().toString();
            Set<Integer> select = mFlowLayout.getSelectedList();

            MsgBean msgBean = new MsgBean();
            msgBean.content = content;
            if (select.iterator().hasNext()) {
                UserBean userBean = list.get(select.iterator().next());
                msgBean.to_id = userBean.id;
                msgBean.to_name = userBean.name;
            }
            boolean isSend = webSocket.send(new Gson().toJson(msgBean));
            if (isSend) {
                edit_insert.setText("");
            } else {
                showToast("链接已关闭");
            }
        } else {
            showToast("初始化失败");
        }
    }

    private void connect(String name, String id) {

        EchoWebSocketListener listener = new EchoWebSocketListener();
        Request request = new Request.Builder()
//                .url("ws://192.168.0.32:8086/select")
//                .url("ws://172.16.2.65:8082")
                .url("ws://172.16.2.65:8080/ws")
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
            MainActivity.this.webSocket = webSocket;
//            webSocket.send("connect_ok");
//            webSocket.send("welcome");
//            webSocket.send(ByteString.decodeHex("adef"));
//            webSocket.close(1000, "再见");
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
            webSocket.close(1000, null);
            output("onClosing: " + code + "/" + reason);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            output("onClosed: " + code + "/" + reason);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            output("onFailure: " + t.getMessage());
        }
    }

    /**
     * 处理数据
     *
     * @param text
     */
    private void manageData(String text) {
        Data data = new Gson().fromJson(text, Data.class);
        if (data != null && data.data_content != null) {
            String content = data.data_content.toString();
            switch (data.data_type) {
                case 1:
                    MsgBean msgBean = new Gson().fromJson(content, MsgBean.class);
                    if (TextUtils.isEmpty(msgBean.to_id)){
                        output(String.format("%s 说：%s", msgBean.from_name, msgBean.content));
                    }else {
                        output(String.format("%s 对你说：%s", msgBean.from_name, msgBean.content));
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
}
