package com.gomez.lingjudemo;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.iflytek.cloud.Setting;
import com.iflytek.cloud.SpeechConstant;
import com.lingju.audio.engine.base.AiEngineService;
import com.lingju.audio.engine.base.RecognizerBase;
import com.lingju.audio.engine.base.SynthesizerBase;
import com.lingju.common.adapter.ChatRobotBuilder;
import com.lingju.common.adapter.LocationAdapter;
import com.lingju.common.adapter.MusicContext;
import com.lingju.common.adapter.NetworkAdapter;
import com.lingju.common.adapter.PropertiesAccessAdapter;
import com.lingju.common.util.BaseCrawler;
import com.lingju.context.entity.AudioEntity;
import com.lingju.context.entity.Command;
import com.lingju.context.entity.Progress;
import com.lingju.context.entity.equip.Deng;
import com.lingju.context.entity.equip.Equipment;
import com.lingju.context.entity.equip.EquipmentGroup;
import com.lingju.context.entity.equip.KongTiao;
import com.lingju.event.InstallIflyteckServiceEvent;
import com.lingju.event.RecognizedEvent;
import com.lingju.event.RecordUpdateEvent;
import com.lingju.event.RobotResponseEvent;
import com.lingju.robot.AndroidChatRobotBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import de.greenrobot.event.EventBus;

/**
 * Created by Administrator on 2015/11/10.
 */
public class MainService extends AiEngineService {
    private final static String TAG="MainService";

    public final static String CMD="cmd";
    public final static String TEXT="text";
    public final static String INT="int";

    /**
     * 启动识别
     */
    public final static int REG_START=1;
    /**
     * 停止识别
     */
    public final static int REG_STOP=2;
    /**
     * 文本合成语音，文本值通过{@linkplain MainService#TEXT}传入
     */
    public final static int SYN_START=3;
    /**
     * 停止合成
     */
    public final static int SYN_STOP=4;
    /**
     * 启动唤醒
     */
    public final static int WAKE_START=5;
    /**
     * 关闭唤醒
     */
    public final static int WAKE_STOP=6;
    /**
     * 向robot输入文本，纯文本输入，输出不会合成语音，文本值通过{@linkplain MainService#TEXT}传入
     */
    public final static int SEND_MSG=7;
    /**
     * 向robot输入文本，该文本理解为语音转译后的文本输入，输出会合成语音，文本值通过{@linkplain MainService#TEXT}传入
     */
    public final static int SEND_VOICE_MSG=8;


    /**
     * 当前是否处于唤醒模式，本demo的唤醒模式定义如下：<br>
     * <p>麦克风处于长期录音状态，一旦监听到用户语音输入了唤醒词，onWakeup()方法将会被回调，监听在以下两种情况下暂时关闭：
     * <p>1.当设备播放音频，如将文本合成语音、播放音乐，这种情况下一般会暂停唤醒监听，但也允许例外：
     *    <p>a.当设备播放的音频不会被自身麦克风录入的情况下，如音频是通过耳塞或者蓝牙耳塞播放({@linkplain MainService#isHeadSet() isHeadSet()}=true或者{@linkplain MainService#isBlueToothHeadSet() isBlueToothHeadSet()}=true)
     *    <p>b.当用户采用自己的唤醒功能，并且该唤醒功能的解决方案非常完美，回声消除做得非常好，在录入自身回声的情况下依然能完美工作，那么将直接实现了语音打断功能，播放时无需暂停唤醒监听
     * <p>2.当设备启动识别功能时，这种情况下无条件暂停唤醒监听
     * <p>注意：系统默认是唤醒一次启动一次识别，如果要唤醒一次连续识别，请参照onCreate()里面的例子
     */
    private volatile boolean awakenMode=true;

    //用于保存机器人属性
    private SharedPreferences preferences;



    @Override
    public void onCreate() {
        addUserwords(getResources().getStringArray(R.array.keywords));
        //addMusic(musics);//传入本地的音乐名称集合
        //addSingers(singers);//传入本地的音乐演唱者集合
        super.onCreate();
        preferences=getSharedPreferences("lingju_sdk", Context.MODE_PRIVATE);
        //创建ChatRobotBuilder及ChatRobot
        AndroidChatRobotBuilder.create(getApplication(),"3c30fa21c178fedf0545552deab3c73b")
               // .setPropertiesAccessAdapter(propertiesAccessAdapter)//机器人属性设置接口已过时，由开放平台提供修改功能
                .setMusicContext(musicContext)
                .setLocationAdapter(locationAdapter)
                .setNetworkAdapter(networkAdapter)
                .build(new ChatRobotBuilder.RobotInitListener() {
                    @Override
                    public void initComplete(int i) {
                        //i=0表示初始化完成，i=-1表示初始化失败
                        if(i==0){
                            //以json的形式更新上传设备信息，须将已有的设备的状态及信息全部上传更新
                            //  String result1 = uploadEquipmentJson();
                            //  System.out.println("上传设备JSON的result:" + result1);
                            /*
                            deme中采用List<Equipment>为参数更新上传设备信息，示例中列举两个设备
                            实际情况要将所有设备一起更新上传
                            */
                            String result2 = uploadEquipmentList();
                            System.out.println("上传设备list的result:" + result2);
                            //以JSONArray为参数上传设备分组信息
                            //String result3 = uploadEquipmentGroupJson();
                            //System.out.println("上传设备groupJson的result:" + result3);
                            /*
                            deme中采用List<EquipmentGroup>为参数更新上传设备分组信息，示例中列举两组设备
                            实际情况要将所有设备分组情况一起更新上传
                            */
                            String result4 = uploadEquipmentGroup();
                            System.out.println("上传设备group的result:" + result4);
                            JSONObject object =AndroidChatRobotBuilder.get().robot().terminalAccessor().getGroup();
                            JSONObject object1 =AndroidChatRobotBuilder.get().robot().terminalAccessor().getLastEquipments();
                        }

                    }
                });
         System.out.println(getResources().getString(R.string.app_id));
        //super.online=false;//监听设备的网络状态并实时更新online的状态


         //如果需要在唤醒模式中实现唤醒后连续语音识别，则放开本段注释
         super.awakenRecognizeTimeout=1000;//唤醒后10秒内无有效的语音交互则系统进入唤醒监听状态，需要重新唤醒才能识别
         super.awakenRecognizeTimeoutTips=new String[]{//唤醒后识别超时的提示播报（随机播报一条），如不设置则无提示，
         "我一边凉快去了",
         "闪了，有事喊我名字",
         "我休息了，与我对话请喊我名字"
         };
         //讯飞sdk的日志级别
         Setting.setLogLevel(Setting.LOG_LEVEL.none);
         //打开讯飞sdk的日志记录，默认关闭
         Setting.setShowLog(false);
         //下调level可输出相应的日志
        RecognizerBase.isInited();
        registerReceiver(netWorkChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        super.setSynthesizeSpeaker("vinn");
    }

    private String uploadEquipmentJson() {
        JSONArray equipmentArray = new JSONArray();
        JSONObject deng1 = new JSONObject();
        JSONObject deng2 = new JSONObject();
        try {
            //equip_id与name需要保证唯一性，且不能为空，否则将报设备找不到
            deng1.put("id",301);
            deng1.put("equip_type",1000);
            deng1.put("equip_id","d1");
            deng1.put("name","灯1");
            deng1.put("alias","厨房灯");
            deng1.put("equip_location","厨房");
            deng1.put("brightness",new Progress("80").toJsonString());
            equipmentArray.put(deng1);
            deng2.put("id",301);
            deng2.put("equip_type",1000);
            deng2.put("equip_id","d2");
            deng2.put("name","灯2");
            deng2.put("alias","厕所灯");
            deng2.put("equip_location","厕所");
            deng2.put("brightness",new Progress("70").toJsonString());
            equipmentArray.put(deng2);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return  AndroidChatRobotBuilder.get().robot().terminalAccessor().upload(equipmentArray);
    }
    private String uploadEquipmentList() {
        final List<Equipment> list = new ArrayList<Equipment>();
        //equip_id与name需要保证唯一性，且不能为空，否则将报设备找不到
        Deng e = new Deng();
        e.setEquip_id("d3");
        e.setBrightness(new Progress("80"));
        e.setName("灯3");
        e.setEquip_location("客厅");
        e.setAlias("客厅灯");
        e.setStatus("OPEN");
        list.add(e);
        e = new Deng();
        e.setBrightness(new Progress("80"));
        e.setEquip_id("d4");
        e.setName("灯4");
        e.setEquip_location("走廊");
        e.setAlias("走廊灯");
        e.setStatus("CLOSE");
        list.add(e);
        KongTiao k = new KongTiao();
        k.setEquip_location("客厅");
        k.setEquip_id("k1");
        k.setName("空调");
        k.setLevel(3);
        k.setStatus("OPEN");
        list.add(k);
        return  AndroidChatRobotBuilder.get().robot().terminalAccessor().upload(list);
    }
    private String uploadEquipmentGroupJson() {
        JSONArray equipmentGroup = new JSONArray();
        JSONArray jsonArray1 = new JSONArray();
        JSONArray jsonArray2 = new JSONArray();
        JSONObject object1 = new JSONObject();
        JSONObject object2 = new JSONObject();
        try {
            object1.put("name","白天");
            jsonArray1.put("x1");
            jsonArray1.put("x2");
            jsonArray1.put("x3");
            object1.put("group",jsonArray1);
            object1.put("timestamp",new java.util.Date().getTime());
            object2.put("name","晚上");
            jsonArray2.put("y1");
            jsonArray2.put("y2");
            jsonArray2.put("y3");
            object2.put("group",jsonArray2);
            object2.put("timestamp",new java.util.Date().getTime());
            equipmentGroup.put(object2);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return  AndroidChatRobotBuilder.get().robot().terminalAccessor().uploadGroup(equipmentGroup);
    }
    private String uploadEquipmentGroup() {
        List<EquipmentGroup> list = new ArrayList<EquipmentGroup>();
        EquipmentGroup g = new EquipmentGroup();
        g.setName("晚上");
        List<String> gids = new ArrayList<String>();
        gids.add("l1");
        gids.add("k1");
        g.setGroup(gids);
        list.add(g);
        g = new EquipmentGroup();
        g.setName("中午");
        gids = new ArrayList<String>();
        gids.add("l2");
        gids.add("k2");
        g.setGroup(gids);
        list.add(g);
        return  AndroidChatRobotBuilder.get().robot().terminalAccessor().uploadGroup(list);
    }
    @Override
    public void onDestroy() {
        unregisterReceiver(netWorkChangeReceiver);
        super.onDestroy();
    }

    @Override
    public void update(Observable observable, Object o) {
        super.update(observable, o);
        this.awakenMode=true;
        startWakeup();
    }

    /**
     * 是否插入了耳机
     * 如果要实现打断功能，只需返回true即可，即持续的开启唤醒监听（包括设备播音时）
     * ps:打断功能的前提是你设备有优秀的回声及消噪处理，本sdk采用AudioTrack录音，
     * 不做任何回声消除(AEC)处理，需确保录音和播音同时进行而互不干扰，即设备自身播放出来的声音被自身的麦克风录进去后能通过硬件或者底层软件AEC模块消除掉）
     * 如果要在android手机上体验该功能，请戴上有线耳机测试
     * @return
     */
    @Override
    public boolean isHeadSet(){
        return false;
    }

    /**
     * 以下为AiEngineService的语音组件初始化方法，用户可重载不替换
     * 初始化讯飞语音组件，如果没有安装语记，将会post一个{@linkplain InstallIflyteckServiceEvent InstallIflyteckServiceEvent}事件<br>
     * <p>请在前端接收该事件并提示用户安装，如果是做定制android系统的，建议预装讯飞的语记，<br>
     * <p>本sdk采用了基于讯飞离线命令词识别做成的唤醒引擎，唤醒引擎在语音识别引擎完成后初始化
     */
   /* protected void initVoiceComponent(){
       Log.i(TAG, "initVoiceComponent>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        SpeechUtility.createUtility(this, SpeechConstant.APPID + "=" + getResources().getString(com.lingju.R.string.app_id));
        com.iflytek.cloud.Setting.setLogLevel(com.iflytek.cloud.Setting.LOG_LEVEL.none);
        com.iflytek.cloud.Setting.setLogPath("/mnt/sdcard/msc_synthesize.log");
        com.iflytek.cloud.Setting.setShowLog(false);
        com.iflytek.cloud.Setting.setSaveTestLog(false);
        if(SpeechUtility.getUtility().checkServiceInstalled ()){
            synthesizer= IflySynthesizer.createInstance(this);
            recognizer= IflyRecognizer.createInstance(this);
            recognizer.addObserver(this);
        }
        else{
           Log.i(TAG, "send InstallIflyteckServiceEvent>>>>>>>>");
            EventBus.getDefault().post(new InstallIflyteckServiceEvent());
        }
    }*/


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent==null)return super.onStartCommand(intent,flags,startId);
        int type = intent.getIntExtra(CMD, -1);
        String text=intent.getStringExtra(TEXT);
        Log.i(TAG,"onStartCommand>>"+type);
        switch(type){
            case REG_START:
                 startRecognize();
                break;
            case REG_STOP:
                stopRecognize();
                break;
            case SYN_START:
                if(!TextUtils.isEmpty(text)&& SynthesizerBase.isInited()){
                    synthesizer.startSpeakAbsolute(text);
                    SynthesizerBase.get().startSpeakAbsolute(text);
                }
                break;
            case SYN_STOP:
                if(synthesizer!=null){
                    synthesizer.stopSpeakingAbsolte();
                }
                break;
            case WAKE_START:
                startWakeup();
                break;
            case WAKE_STOP:
                stopWakenup();
                break;
            case SEND_MSG:
                if(!TextUtils.isEmpty(text))
                    sendMessageToRobot(text,0);
                break;
            case SEND_VOICE_MSG:
                if(!TextUtils.isEmpty(text))
                    System.out.println("发送给机器人");
                    sendMessageToRobot(text,1);
                break;
            default:break;
        }
        return super.onStartCommand(intent, Service.START_STICKY, startId);
    }


    /**
     * 音乐播放的上下文接口.<br>
     *<p> 实现该接口是为了让聊天机器人能够随时获取当前播放的音频文件的信息
     *
     */
    MusicContext musicContext=new MusicContext() {
        /**
         * 获取当前播放歌曲的名字
         * @return 若无返回null
         */
        public String getName(){
            //...
            return null;
        }
        /**
         * 获取当前播放歌曲的演唱歌手
         * @return 若无返回null
         */
        public String getSinger(){
            //...
            return null;
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
         * <p> 如果当前播放的歌曲是在线歌曲，返回MusicId,如果是本地音频，返回音频文件的绝对路径
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

    protected void sendResonse(String text,int inputType){
        if(inputType==RobotResponseEvent.TYPE_SYNTHESIZE){
            synthesizer.startSpeakAbsolute(new SynthesizerBase.SpeechMessage(text).setOrigin(SynthesizerBase.ORIGIN_COMMON));
        }
        else if(inputType==RobotResponseEvent.TYPE_APPEND_SYNTHESIZE){
            synthesizer.addMessage2Speak(new SynthesizerBase.SpeechMessage(text).setOrigin(SynthesizerBase.ORIGIN_COMMON));
        }
        EventBus.getDefault().post(new RobotResponseEvent(text, inputType));
    }

    @Override
    protected void onRobotResponse(String text, Command cmd, int type) {
        setRecognizeParams(SpeechConstant.LANGUAGE,"zh_cn");
        EventBus.getDefault().post(new RecordUpdateEvent(RecordUpdateEvent.RECORD_IDLE));
        SynthesizerBase.setCompletedMode(awakenMode ? SynthesizerBase.AWAKEN_LISTEN_COMPLETED : SynthesizerBase.DO_NOTHING_COMPLETED);
        super.onRobotResponse(text, cmd, type);

        Log.i(TAG, "onResult>>text="+text+",cmd:" + cmd.toJsonString());

        //获取登陆状态信息，未登陆返回的状态码请参见安卓sdk接入文档的错误码表
        Log.i(TAG,"loginMessage:"+cmd.getLoginMessage());
        String motions=cmd.getMotions();
        if(!TextUtils.isEmpty(motions)){
            //合成文本并执行动作列表
        }
        //获取语义对象集合，此处为获取musicId并播放歌曲示例：
        String actions = cmd.getActions();
        if(!TextUtils.isEmpty(actions)){
            //语义对象集合
            try {
                JSONArray jsonArray = new JSONArray(actions);
                if(jsonArray!=null){
                  for(int i =0;i<jsonArray.length();i++){
                      //获取语义动作
                      if(!jsonArray.getJSONObject(i).isNull("action")){
                          jsonArray.getJSONObject(i).getString("action");
                          Log.i(TAG,  "action :"+jsonArray.getJSONObject(i).getString("action"));
                      }
                      //获取动作执行时间
                      if(!jsonArray.getJSONObject(i).isNull("etime")){
                         jsonArray.getJSONObject(i).getString("etime");
                         Log.i(TAG,"etime :"+jsonArray.getJSONObject(i).getString("etime"));
                      }
                      //获取语义动作目标对象
                      if(!jsonArray.getJSONObject(i).isNull("target")){
                          String target = jsonArray.getJSONObject(i).getString("target");
                          Log.i(TAG,  "target :"+target);
                          JSONObject targetObject = new JSONObject(target);
                          if(!targetObject.isNull("object")){
                              String object = targetObject.getString("object");
                              JSONArray objectArray = new JSONArray(object);
                              if(objectArray!=null){
                                  for(i=0;i<objectArray.length();i++){
                                      if(!objectArray.getJSONObject(i).isNull("musicId")){
                                          String musicId = objectArray.getJSONObject(i).getString("musicId");
                                          Log.i(TAG,  "musicId :"+musicId);
                                          //将musicId添加到音乐列表
                                          AudioEntity audioEntity = new AudioEntity();
                                          audioEntity.setMusicId(musicId);
                                          super.currentResult.getMusicList().add(audioEntity);
                                          //获取音乐播放地址，并播放音乐,请另起线程执行
                                          String palyUrl = new BaseCrawler().getMusicUri(musicId);
                                          Log.i(TAG,  "palyUrl :"+palyUrl);
                                      }
                                  }

                              }
                          }

                      }
                  }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        sendResonse(text, type);

    }

    @Override
    public void onDefaultResponse(String text, Command cmd, int type) {
        sendResonse(text, type);
    }

    @Override
    public void onAudioActionResponse(String text, Command cmd, int type) {

        if(!TextUtils.isEmpty(text)){
            sendResonse(text,type);
        }
    }

    @Override
    public void onPlayResponse(String text, Command cmd, int type) {
        sendResonse(text,type);
    }

    @Override
    public void onWakeResponse(String text, Command cmd, int type) {
        sendResonse(text,type);
    }


    @Override
    public void onRecoginzeVolumeChanged(int v) {

    }

    @Override
    public void onRecoginzeResult(String result) {
        EventBus.getDefault().post(new RecordUpdateEvent(RecordUpdateEvent.RECORD_IDLE_AFTER_RECOGNIZED));
        if(TextUtils.isEmpty(result)){
            result="我似乎什么都没听到";
            sendResonse(result,1);
        }
        else {
            EventBus.getDefault().post(new RecognizedEvent(result));
            sendMessageToRobot(result, 1);
        }
    }


    @Override
    public void startRecognize() {
        super.awakenTime=System.currentTimeMillis();
        super.startRecognize();
    }

    @Override
    public void awakenRecognizeTimeout() {
        Log.i(TAG,"awakenRecognizeTimeout");
    }

    @Override
    public void onRecognizeError(int e) {
        super.onRecognizeError(e);
    }

    @Override
    public void onRecoginzeBegin() {
        EventBus.getDefault().post(new RecordUpdateEvent(RecordUpdateEvent.RECORDING));
    }

    @Override
    public void onRecoginzeEnd() {
        EventBus.getDefault().post(new RecordUpdateEvent(RecordUpdateEvent.RECOGNIZING));
    }

    @Override
    public void onSynthesizerInited(int errorCode) {
        Log.i(TAG,"onSynthesizerInited"+errorCode);
    }

    @Override
    public void onSynthersizeCompleted(int errorCode) {
        Log.i(TAG,"onSynthersizeCompleted"+errorCode);
    }

    @Override
    public void onSynthersizeSpeakBegin() {
        Log.i(TAG,"onSynthersizeSpeakBegin");
    }

    @Override
    protected void onSynthersizeError(int errorCode) {
        Log.i(TAG,"onSynthersizeError>>"+errorCode);
    }

    @Override
    public void onSpeakProgress(int p) {

    }

    public boolean isWakeUpMode() {
        return awakenMode;
    }

    @Override
    public void onWakeup(String result) {
        Log.i(TAG,"onWakeup");
        //唤醒后启动识别
        startRecognize();
    }

    @Override
    public void sendMessageToRobot(String text, int type) {
        if(AndroidChatRobotBuilder.get().isRobotCreated()){
            AndroidChatRobotBuilder.get().robot().process(text,new RobotResponseCallBack(type));
        }
    }

    @Override
    public boolean isPlaying() {
        return false;
    }

    @Override
    public void pausePlay() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private BroadcastReceiver netWorkChangeReceiver =new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
           Log.i(TAG, "netWorkChangeReceiver");
            ConnectivityManager connectivityManager=(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            //NetworkInfo mobNetInfo=connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            NetworkInfo  wifiNetInfo=connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            //MainService.this.online=wifiNetInfo.isConnected() || mobNetInfo.isConnected();
            //if (!mobNetInfo.isConnected() && !wifiNetInfo.isConnected()) {
            if (!wifiNetInfo.isConnected()){
                MainService.this.online=false;
                Log.w(TAG,"netWorkChangeReceiver>>>offline");
                if(synthesizer!=null){
                    synthesizer.setForceLocalEngine(true);
                }
            }else {
                MainService.this.online=true;
                Log.w(TAG,"netWorkChangeReceiver>>>online");
                if(synthesizer!=null){
                    synthesizer.setForceLocalEngine(false);
                }
            }
            networkAdapter.notifyUpdate();
        }
    };

    @Override
    public void stopRecognize() {
        super.awakenTime=0;
        super.stopRecognize();
    }

    /**
     * 此接口已过时，机器人属性修改由开放平台提供
     */
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
            preferences.edit().putString("robotName","小灵").commit();
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
            return  MainService.this.online;
        }

        @Override
        public NetType currentNetworkType() {
            return null;
        }
    };

    private LocationAdapter locationAdapter=new LocationAdapter() {

        @Override
        public double getCurLng() {
            return 114.047447;
        }

        @Override
        public double getCurLat() {
            return 22.620224;
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

}
