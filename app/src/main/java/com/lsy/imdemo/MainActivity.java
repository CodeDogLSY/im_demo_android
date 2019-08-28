package com.lsy.imdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
    private TextView tv_enter;
    private WebSocket webSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        start = findViewById(R.id.start);
        text = findViewById(R.id.text);
        edit_insert = findViewById(R.id.edit_insert);
        tv_enter = findViewById(R.id.tv_enter);
        tv_enter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onEnter();
            }
        });

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connect(StrUtil.getRandomName(),StrUtil.getRandomId());
            }
        });
    }





    /**
     * 发送消息
     */
    private void onEnter() {
        if (webSocket != null) {
            boolean isSend = webSocket.send(edit_insert.getText().toString());
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
        public void onMessage(WebSocket webSocket, String text) {
            output("onMessage: " + text);
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


    private void output(final String content) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text.setText(text.getText().toString() + content + "\n");
            }
        });
    }
}
