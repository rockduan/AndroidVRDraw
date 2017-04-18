package com.dlodlo.admin.dlodlovrdraw;


import android.app.Activity;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.dlodlo.admin.dlodlovrdraw.glUtils.MyRenderer;


public class MainActivity extends Activity {
    public static final String TAG = "Grafika";
    private Button btn1 ;
    private Button btn2 ;
    private Button btn3 ;
    private Button btn4;
    private Intent intent_double_decode ;
    private Intent intent_multi_surface ;
    private Intent intent_texture_from_camera;
    private Intent intent_picture_draw;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

       // rh.sendTexturId(myRenderer.getmTextures()[0]);
        //show MainActivity
        setContentView(R.layout.activity_main);
        // One-time singleton initialization; requires activity context to get file location.
        ContentManager.initialize(this);
        intent_double_decode = new Intent(this,DoubleDecodeActivity.class);
        intent_multi_surface = new Intent(this,MultiSurfaceActivity.class);
        intent_texture_from_camera = new Intent(this,TextureFromCameraActivity.class);
        intent_picture_draw = new Intent(this, PictureActivity.class);
        ContentManager cm = ContentManager.getInstance();
        //if (!cm.isContentCreated(this)) {
        //    Log.d(TAG,"!cm.isContentCreated(this)");
        //    ContentManager.getInstance().createAll(this);
       // }else{
       //     Log.d(TAG,"Content has been Created");
       // }
        btn1 = (Button)findViewById(R.id.button);
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(intent_double_decode);
            }
        });
        btn2 = (Button)findViewById(R.id.button2);
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(intent_multi_surface);
            }
        });
        btn3 = (Button)findViewById(R.id.button3);
        btn3.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                startActivity(intent_texture_from_camera);
            }
        });
        btn4 = (Button)findViewById(R.id.button4);
        btn4.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                startActivity(intent_picture_draw);
            }
        });
    }
}
