package com.gomez.lingjudemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.lingju.audio.engine.base.RecognizerBase;
import com.lingju.event.InstallIflyteckServiceEvent;
import com.lingju.event.RecognizedEvent;
import com.lingju.event.RecordUpdateEvent;
import com.lingju.event.RobotResponseEvent;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;


public class MainCommonActivity extends Activity{
    private final static String TAG="MainActivity";

    private EditText inputEdit;
    private Button sendButton;
    private Button voiceButton;
    private RecyclerView recyclerView;
    private ChatHistoryAdapter adater;
    private final List<Msg> history=new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_common);

        inputEdit=(EditText)findViewById(R.id.main_common_input_edit);
        sendButton=(Button)findViewById(R.id.main_common_send_bt);
        voiceButton=(Button)findViewById(R.id.main_common_voice_input_bt);
        recyclerView= (RecyclerView) findViewById(R.id.main_common_recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adater=new ChatHistoryAdapter();
        recyclerView.setAdapter(adater);
        EventBus.getDefault().register(this);



        voiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!RecognizerBase.isInited())return;
                Intent intent=new Intent(MainCommonActivity.this,MainService.class);
                if(RecognizerBase.get().isRecognizing()){
                    intent.putExtra(MainService.CMD,MainService.REG_STOP);
                    startService(intent);
                }
                else{
                    intent.putExtra(MainService.CMD,MainService.REG_START);
                    startService(intent);
                }
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                if(TextUtils.isEmpty(inputEdit.getText())){
                    Toast.makeText(MainCommonActivity.this,"请输入中文文本",Toast.LENGTH_LONG).show();
                }
                else{
                    String input=inputEdit.getText().toString();
                    inputEdit.setText("");
                    history.add(new Msg(input, Msg.INPUT_TYPE));
                    adater.notifyDataSetChanged();
                    recyclerView.scrollToPosition(history.size() - 1);
                    Intent intent=new Intent(MainCommonActivity.this,MainService.class);
                    intent.putExtra(MainService.CMD,MainService.SEND_MSG);
                    intent.putExtra(MainService.TEXT,input);
                    startService(intent);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    public void onEventMainThread(RobotResponseEvent e){
        if(TextUtils.isEmpty(e.getText()))return;
        history.add(new Msg(e.getText(), Msg.OUTPUT_TYPE));
        adater.notifyDataSetChanged();
        recyclerView.scrollToPosition(history.size() - 1);
    }

    public void onEventMainThread(RecordUpdateEvent e){
        switch(e.getState()){
            case RecordUpdateEvent.RECORD_IDLE:
                //voiceButton.setRecordIdleState();
                voiceButton.setText("点击说话");
                break;
            case RecordUpdateEvent.RECORDING:
                //voiceButton.setRecordStartState();
                voiceButton.setText("录音中...");
                break;
            case RecordUpdateEvent.RECOGNIZING:
               // voiceButton.setRecognizeCompletedState();
                voiceButton.setText("识别中...");
                break;
            case RecordUpdateEvent.RECORD_IDLE_AFTER_RECOGNIZED:
                voiceButton.setText("思考中...");
                break;
            default:break;
        }
    }

    public void onEventMainThread(RecognizedEvent e){
        if(TextUtils.isEmpty(e.getText()))return;
        history.add(new Msg(e.getText(), Msg.INPUT_TYPE));
        adater.notifyDataSetChanged();
        recyclerView.scrollToPosition(history.size() - 1);
    }

    public void onEventMainThread(InstallIflyteckServiceEvent e){
        //安装讯飞的语记

    }


    class ChatHistoryItem extends RecyclerView.ViewHolder{
        //0=靠右的输入文本 1=靠左的输出文本
        int type;
        TextView textView;

        public ChatHistoryItem(TextView itemView,int type) {
            super(itemView);
            this.textView=itemView;
            this.type=type;
        }
    }

    class ChatHistoryAdapter extends RecyclerView.Adapter<ChatHistoryItem>{
        @Override
        public ChatHistoryItem onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView textView=new TextView(MainCommonActivity.this);
            textView.setSingleLine(false);
            textView.setPadding(0,10,0,10);
            ViewGroup.LayoutParams layoutParams=new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
            parent.addView(textView,layoutParams);
            return new ChatHistoryItem(textView,viewType);
        }


        @Override
        public void onBindViewHolder(ChatHistoryItem holder, int position) {
            Msg msg=history.get(position);
            holder.textView.setText(Html.fromHtml(msg.getMessage()));
            holder.type=msg.getType();
            if(holder.type==0){
                holder.textView.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
                holder.textView.setTextColor(getResources().getColor(android.R.color.holo_green_light));
            }
            else{
                holder.textView.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
                holder.textView.setTextColor(getResources().getColor(android.R.color.black));
            }
        }

        @Override
        public int getItemCount() {
            return history.size();
        }
    }

    static class Msg {
        public static final int INPUT_TYPE=0;
        public static final int OUTPUT_TYPE=1;

        private String message;
        private int type;

        public Msg(String message, int type) {
            this.message = message;
            this.type = type;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }
    }

}
