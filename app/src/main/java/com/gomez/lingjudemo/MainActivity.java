package com.gomez.lingjudemo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.lingju.common.adapter.ChatRobotBuilder;
import com.lingju.common.adapter.LocationAdapter;
import com.lingju.common.adapter.MusicContext;
import com.lingju.common.adapter.NetworkAdapter;
import com.lingju.common.adapter.PropertiesAccessAdapter;
import com.lingju.common.callback.ResponseCallBack;
import com.lingju.common.util.BaseCrawler;
import com.lingju.context.entity.AudioEntity;
import com.lingju.context.entity.base.IChatResult;
import com.lingju.robot.AndroidChatRobotBuilder;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity implements ResponseCallBack {
    private final static String TAG="MainActivity";

    private EditText inputEdit;
    private Button sendButton;
    private RecyclerView recyclerView;
    private ChatHistoryAdapter adater;
    private final List<Msg> history=new ArrayList<>();
    private BaseCrawler crawler=new BaseCrawler();
    //用于保存机器人属性
    private SharedPreferences preferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputEdit=(EditText)findViewById(R.id.main_input_edit);
        sendButton=(Button)findViewById(R.id.main_send_bt);
        recyclerView= (RecyclerView) findViewById(R.id.main_recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adater=new ChatHistoryAdapter();
        recyclerView.setAdapter(adater);
        preferences=getSharedPreferences("lingju_sdk", Context.MODE_PRIVATE);

        AndroidChatRobotBuilder.create(getApplication(),"3c30fa21c178fedf0545552deab3c73b")
                .setPropertiesAccessAdapter(propertiesAccessAdapter)
                .setMusicContext(musicContext)
                .setLocationAdapter(locationAdapter)
                .setNetworkAdapter(networkAdapter)
                .build(new ChatRobotBuilder.RobotInitListener() {
                    @Override
                    public void initComplete(int i) {

                    }
                });

        sendButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                if(TextUtils.isEmpty(inputEdit.getText())){
                    Toast.makeText(MainActivity.this,"请输入中文文本",Toast.LENGTH_LONG).show();
                }
                else{
                    if(!AndroidChatRobotBuilder.get().isRobotCreated()){
                        Toast.makeText(MainActivity.this,"灵聚智能引擎尚未初始化",Toast.LENGTH_LONG).show();
                        return;
                    }
                    String input=inputEdit.getText().toString();
                    inputEdit.setText("");
                    history.add(new Msg(input, Msg.INPUT_TYPE));
                    adater.notifyDataSetChanged();
                    recyclerView.scrollToPosition(history.size() - 1);
                    AndroidChatRobotBuilder.get().robot().process(input,MainActivity.this);
                }
            }
        });
    }


    private PropertiesAccessAdapter propertiesAccessAdapter=new PropertiesAccessAdapter() {
        @Override
        public void saveUserName(String s) {
            //持久化用户为自己设置的名字
            Log.i(TAG,"saveUserName>>"+s);
            preferences.edit().putString("userName",s).commit();
        }

        @Override
        public String getUserName() {
            //获取用户为自己设置的名字
            Log.i(TAG,"getUserName>>");
            return preferences.getString("userName","主人");
        }

        @Override
        public void saveRobotName(String s) {
            //保存用户设置的机器人名字
            Log.i(TAG,"saveRobotName>>"+s);
            preferences.edit().putString("robotName",s).commit();
        }

        @Override
        public String getRobotName() {
            //获取用户为机器人设置的名字
            Log.i(TAG,"getRobotName>>");
            return preferences.getString("robotName","小灵");
        }

        @Override
        public void saveGender(int i) {
            //保存用户设置的机器人的性别
            Log.i(TAG,"saveGender>>"+i);
            preferences.edit().putInt("gender",i).commit();

        }

        @Override
        public String getGender() {
            //获取用户设置的机器人的性别
            Log.i(TAG,"getGender>>");
            int g=preferences.getInt("gender",3);
            if(g==0)return "我是男孩";
            else if(g==1)return "我是女孩";
            return "机器人是没有性别的";
        }

        @Override
        public void saveBirthday(Date date) {
            //保存用户设置的机器人的出生年月日
            Log.i(TAG,"saveBirthday>>"+date.toString());
            preferences.edit().putLong("birthday",date.getTime()).commit();
        }

        @Override
        public Date getBirthday() {
            //获取用户设置的机器人的出生年月日
            Log.i(TAG,"getBirthday>>");
            return new Date(preferences.getLong("birthday",System.currentTimeMillis()-3600000*365));
        }

        @Override
        public void saveParent(String s) {
            //保存用户设置的机器人的父母
            Log.i(TAG,"saveParent>>"+s);
            preferences.edit().putString("parent",s).commit();
        }

        @Override
        public String getParent() {
            //获取用户设置的机器人的父母
            Log.i(TAG,"getParent>>");
            return preferences.getString("parent","上帝");
        }

        @Override
        public void saveFather(String s) {
            Log.i(TAG,"saveFather>>"+s);
            preferences.edit().putString("father",s).commit();

        }

        @Override
        public void saveMother(String s) {
            Log.i(TAG,"saveMother>>"+s);
            preferences.edit().putString("mother",s).commit();
        }

        @Override
        public String getFather() {
            Log.i(TAG,"getFather>>");
            return preferences.getString("father","天父");
        }

        @Override
        public String getMother() {
            Log.i(TAG,"getMother>>");
            return preferences.getString("mother","圣母玛利亚");
        }

        @Override
        public String getWeight() {
            Log.i(TAG,"getWeight>>");
            //不要不加中文单位，返回的值就是要朗读或者显示的文本
            return preferences.getString("weight","10千克");
        }

        @Override
        public String getHeight() {
            Log.i(TAG,"getHeight>>");
            return preferences.getString("height","我有30公分高");
        }

        @Override
        public String getMaker() {
            Log.i(TAG,"getMaker>>");
            return preferences.getString("maker","灵聚科技");
        }

        @Override
        public String getBirthplace() {
            Log.i(TAG,"getBirthplace>>");
            return preferences.getString("birthplace","广州");
        }

        @Override
        public String getIntroduce() {
            Log.i(TAG,"getIntroduce>>");
            return preferences.getString("introduce","我啥都不会，就一话唠");
        }
    };


    private NetworkAdapter networkAdapter=new NetworkAdapter() {
        @Override
        public boolean isOnline() {
            return true;
        }

        @Override
        public NetType currentNetworkType() {
            return null;
        }
    };


    private LocationAdapter locationAdapter=new LocationAdapter() {

        @Override
        public double getCurLng() {
            return 113.954334;
        }

        @Override
        public double getCurLat() {
            return 22.560235;
        }

        @Override
        public String getCurCity() {
            return "深圳市";
        }

        @Override
        public String getCurAddressDetail() {
            return "广东省深圳市";
        }
    };


    /**
     * 该方法在后台线程执行，请勿在该线程更新ui
     * @param result
     */
    @Override
    public void onResult(IChatResult result) {
        Log.i(TAG, "onResult>>text="+result.getText()+",cmd:" + result.cmd().toJsonString());
        //获取登陆状态信息，未登陆返回的状态码请参见安卓sdk接入文档的错误码表
        Log.i(TAG,"loginMessage:"+result.cmd().getLoginMessage());
        if(result.getStatus()!=IChatResult.Status.SUCCESS){
            Log.i(TAG,"应答出错："+result.getStatus().toString());
            return;
        }
        String motions=result.cmd().getMotions();
        if(!TextUtils.isEmpty(motions)){
            //合成文本并执行动作列表
        }
        String actions = result.cmd().getActions();
        if(!TextUtils.isEmpty(actions)){
            //语义对象集合
        }

        history.add(new Msg(result.getText(), Msg.OUTPUT_TYPE));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adater.notifyDataSetChanged();
                recyclerView.scrollToPosition(history.size()-1);
            }
        });
    }



    /**
     * 音乐播放的上下文接口.
     * 实现该接口是为了让聊天机器人能够随时获取当前播放的音频文件的信息
     *
     */
    MusicContext musicContext=new MusicContext() {
        /**
         * 获取当前播放歌曲的名字
         * @return 若无返回null
         */
        public String getName(){
            //...
            return "未知歌曲";
        }
        /**
         * 获取当前播放歌曲的演唱歌手
         * @return 若无返回null
         */
        public String getSinger(){
            //...
            return "未知歌手";
        }
        /**
         * 获取当前播放歌曲所属的专辑名称
         * @return 若无返回null
         */
        public String getAlums(){
            //...
            return null;
        }
        /**
         * 获取当前播放歌曲的的MusicId<br>
         * 如果当前播放的歌曲是在线歌曲，返回MusicId,如果是本地音频，返回音频文件的绝对路径
         * @return 若无返回null
         */
        public String getMusicId(){
            //...
            return null;
        }
        /**
         * 根据歌曲名获取本地对应的歌曲集合
         * @param name 歌曲名，需判空
         * @return 若无返回空list，不能为null
         */
        public List<AudioEntity> getMusicByName(String name){
            //...
            return new ArrayList<AudioEntity>();
        }
        /**
         * 根据歌手获取本地对应歌手的所有歌曲集合
         * @param singer 歌手名，需判空
         * @return 若无返回空list，不能为null
         */
        public List<AudioEntity> getMusicBySinger(String singer){
            //...
            return new ArrayList<AudioEntity>();
        }
        /**
         * 获取本地对应专辑的所有歌曲集合
         * @param album 专辑名，需判空
         * @return 若无返回空list，不能为null
         */
        public List<AudioEntity> getMusicByAlbum(String album){
            //...
            return new ArrayList<AudioEntity>();
        }
        /**
         * 获取本地对应歌曲名+歌手的歌曲集合
         * @param name 歌名，需判空
         * @param singer 歌手，需判空
         * @return 若无返回空list，不能为null
         */
        public List<AudioEntity> getMusicByNameAndSinger(String name,String singer){
            //...
            return new ArrayList<AudioEntity>();
        }
        /**
         * 获取本地对应歌曲名+专辑名的歌曲实体集合
         * @param name 歌曲名，需判空
         * @param album 专辑名，需判空
         * @return 若无返回空list，不能为null
         */
        public List<AudioEntity> getMusicByNameAndAlbum(String name,String album){
            //...
            return new ArrayList<AudioEntity>();
        }
        /**
         * 根据歌手或者歌名获取本地对应歌曲集合
         * @param str 歌名or歌手，需判空
         * @return 若无返回空list，不能为null
         */
        public List<AudioEntity> getMusicByNameOrSinger(String str){
            //...
            return new ArrayList<AudioEntity>();
        }
        /**
         * 当前播放列表歌曲是否是在线歌曲
         * @return true：在线，false：离线
         */
        public boolean isOnlineMC(){
            //请维护当前播放歌曲的信息，返回真实的情况
            return true;
        }
        /**
         * 判断手机里是否有歌曲
         * @return true：有，false：没有
         */
        public boolean hasMusic(){
            //检索手机里的本地歌曲，请按实际返回
            return false;
        }
    };


    class ChatHistoryItem extends RecyclerView.ViewHolder{
        TextView textView;

        public ChatHistoryItem(TextView itemView) {
            super(itemView);
            this.textView=itemView;
        }
    }

    class ChatHistoryAdapter extends RecyclerView.Adapter<ChatHistoryItem>{
        @Override
        public ChatHistoryItem onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView textView=new TextView(MainActivity.this);
            textView.setSingleLine(false);
            textView.setPadding(0,10,0,10);
            ViewGroup.LayoutParams layoutParams=new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
            parent.addView(textView,layoutParams);
            if(viewType==0){
                textView.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
                textView.setTextColor(getResources().getColor(android.R.color.holo_green_light));
            }
            else{
                textView.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
                textView.setTextColor(getResources().getColor(android.R.color.black));
            }
            return new ChatHistoryItem(textView);
        }


        @Override
        public void onBindViewHolder(ChatHistoryItem holder, int position) {
            holder.textView.setText(Html.fromHtml(history.get(position).getMessage()));
        }

        @Override
        public int getItemViewType(int position) {
            return history.get(position).getType();
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
