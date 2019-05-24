package com.app.beacon;
import android.content.Intent;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.app.beacon.M.Bean.OAuthBean;
import com.app.beacon.M.Bean.measurementBean;
import com.app.beacon.M.Bean.userBean;
import com.app.beacon.P.GetCurrentLocationPresenter;
import com.app.beacon.P.GetSVGPersenter;
import com.app.beacon.P.GetUserInfoPersenter;
import com.app.beacon.Util.HttpUtil;
import com.app.beacon.V.IMainView;
import com.google.gson.Gson;
import com.itheima.library.PhotoView;
import com.mingle.widget.ShapeLoadingDialog;
import com.pixplicity.sharp.Sharp;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements IMainView, View.OnClickListener {
    private GetUserInfoPersenter getUserInfoPersenter;
    private GetSVGPersenter getSVGPersenter;
    private GetCurrentLocationPresenter getCurrentLocationPresenter;
    private ImageView open,img_user,img_openothers,img_closeothers;
    private PhotoView view;
    private DrawerLayout drawerLayout;
    private ShapeLoadingDialog shapeLoadingDialog;
    private OAuthBean bean;
    private String email;
    private Button logout,about;
    private TextView tv_email,tv_Device,tv_Category,tv_Client;
    private userBean userbean;
    private measurementBean measurementbean;
    private StringBuffer SVGBUFFER;
    private boolean FLAG = false;
    private boolean TIMER_FLAG = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        bean = (OAuthBean) getIntent().getSerializableExtra("oauthbean");
        email = getIntent().getStringExtra("email");
        shapeLoadingDialog = new ShapeLoadingDialog(this);
        getUserInfoPersenter = new GetUserInfoPersenter(this);
        initView();
        String url = "https://app.senseagent.com/api/v1/tracking/user/beacon/details?access_token="+bean.getAccess_token();
        getUserInfoPersenter.getUserInfo(url);
    }
    private void initView() {
        open = findViewById(R.id.open);
        img_user = findViewById(R.id.user);
        img_openothers = findViewById(R.id.other_open);
        img_closeothers = findViewById(R.id.other_close);
        tv_email = findViewById(R.id.email);
        tv_Device = findViewById(R.id.Device);
        tv_Category = findViewById(R.id.Category);
        tv_Client = findViewById(R.id.Client);
        drawerLayout = findViewById(R.id.drawer);
        view = findViewById(R.id.view);
        about = findViewById(R.id.about);
        logout = findViewById(R.id.logout);
        logout.setOnClickListener(this);
        open.setOnClickListener(this);
        img_user.setOnClickListener(this);
        img_openothers.setOnClickListener(this);
        img_closeothers.setOnClickListener(this);
        about.setOnClickListener(this);
        view.enable();
    }
    @Override
    public void getUserSuccessView(String jsonstr) {
        Log.v("msgmsgdata",jsonstr);
        Gson gson = new Gson();
        userbean = gson.fromJson(jsonstr,userBean.class);
        if(!FLAG){
            FLAG = true;
            Log.v("msgmsgbean",userbean.toString());
            tv_email.setText(email);
            tv_Device.setText(userbean.getDevice());
            tv_Category.setText(userbean.getCategory());
            tv_Client.setText(userbean.getClient());
            shapeLoadingDialog.dismiss();
            getSVGPersenter = new GetSVGPersenter(this);
            getSVGPersenter.getSvg("https://app.senseagent.com/api/v1/floorplan/download/image/"+userbean.getLastSeen().getLocation().getFloorPlan().getId()+"?access_token="+bean.getAccess_token());
        }else{
            //Work out ratio of pixels per centimetres
            double ratio = measurementbean.getScale().getPixels()/measurementbean.getScale().getCentimeters();
            // Computation Xcm Ycm
            double X = userbean.getLastSeen().getLocation().getFloorPlan().getPosition().getX()*100.0*ratio;
            double Y = userbean.getLastSeen().getLocation().getFloorPlan().getPosition().getY()*100.0*ratio;
            StringBuffer buffer = new StringBuffer(SVGBUFFER);
            int index = buffer.indexOf("</svg>");
            buffer.insert(index,"<circle cx=\""+X+"\" cy=\""+Y+"\" r=\"17\" fill=\"#3ba3d0\"></circle>");
            Sharp.loadString(buffer.toString()).into(view);
            shapeLoadingDialog.dismiss();
        }
    }

    @Override
    public void getUserFailedView(String res){
        shapeLoadingDialog.dismiss();
    }

    @Override
    public void getSVGSuccessView(String svgstr) {
        Log.v("msgmsgres",svgstr);
        SVGBUFFER = new StringBuffer(svgstr);
        Sharp.loadString(svgstr).into(view);
        shapeLoadingDialog.dismiss();
        getCurrentLocationPresenter = new GetCurrentLocationPresenter(this);
        getCurrentLocationPresenter.getCurrentLocation("https://app.senseagent.com/api/v1/floorplan/scale/measurement/get/"+userbean.getLastSeen().getLocation().getFloorPlan().getId()+"?access_token="+bean.getAccess_token());
    }

    @Override
    public void getSVGFailedView(String url) {
        shapeLoadingDialog.dismiss();
    }

    @Override
    public void getCurrentLocationSuccessView(String jsonstr){
        Gson gson = new Gson();
        measurementbean = gson.fromJson(jsonstr,measurementBean.class);
        shapeLoadingDialog.dismiss();
    }

    @Override
    public void getCurrentLocationFailedView(String res){
        Toast.makeText(MainActivity.this,"Load error",Toast.LENGTH_LONG).show();
        shapeLoadingDialog.dismiss();
    }
    @Override
    public void startProgress(String str) {
        shapeLoadingDialog.setLoadingText(str);
        shapeLoadingDialog.show();
    }
    @Override
    public void HiddenProgress() {

    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.open:
                drawerLayout.openDrawer(Gravity.LEFT);
                break;
            case R.id.user:
                if(!TIMER_FLAG){
                    shapeLoadingDialog.setLoadingText("current location start()...");
                    shapeLoadingDialog.show();
                    TimerTask task = new TimerTask() {
                        @Override
                        public void run() {
                            HttpUtil.sendRequestWithOkhttp("https://app.senseagent.com/api/v1/tracking/user/beacon/details?access_token="+bean.getAccess_token(), new Callback() {
                                @Override
                                public void onFailure(Call call, IOException e) {
                                    getUserFailedView("Request error, check network");
                                }
                                @Override
                                public void onResponse(Call call, Response response) throws IOException {
                                    String jsonstr = response.body().string();
                                    getUserSuccessView(jsonstr);
                                }
                            });
                        }
                    };
                    Timer timer = new Timer();
                    long delay = 0;
                    long intevalPeriod = 1*5000;
                    timer.scheduleAtFixedRate(task,delay,intevalPeriod);
                    TIMER_FLAG = true;
                }

                break;
            case R.id.other_open:
                img_openothers.setVisibility(View.GONE);
                img_closeothers.setVisibility(View.VISIBLE);
                break;
            case R.id.other_close:
                img_closeothers.setVisibility(View.GONE);
                img_openothers.setVisibility(View.VISIBLE);
                break;
            case R.id.about:
                Intent goaabout = new Intent(MainActivity.this,AboutActivity.class);
                startActivity(goaabout);
                break;
            case R.id.logout:
                finish();
                break;
        }
    }
}
