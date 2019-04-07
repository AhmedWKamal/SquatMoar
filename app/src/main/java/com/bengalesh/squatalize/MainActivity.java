package com.bengalesh.squatalize;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    ImageView capturedImageView;
    Bitmap bmFrame;
    Bitmap backUpImage;
    Button bReset;
    Uri file;
    Button bUpload;
    int PointsDefined = 0;
    PointF ListOfPoints[] = new PointF[5];
    private StorageReference mStorageRef;
    static final int REQUEST_VIDEO_CAPTURE = 1;

    View.OnTouchListener touchListener = new View.OnTouchListener()
    {
        @Override
        public boolean onTouch(View v, MotionEvent event)
        {
            if(PointsDefined<5){
                capturedImageView.setImageBitmap(drawCircleOnImage(bmFrame,event.getX(),event.getY()));
                bReset.setEnabled(true);
                if(PointsDefined==5){
                    bUpload.setEnabled(true);
                }
            }
            //else{
                //for(int i = 0; i<5;i++){
                    //Log.i("App","Point "+i+": X = "+ListOfPoints[i].x+", Y = "+ListOfPoints[i].y);
                //}
            //}
            return false;
        }
    };
    public Bitmap drawCircleOnImage(Bitmap img, float x, float y){
        Bitmap bitmap = Bitmap.createBitmap(img.getWidth(), img.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(img,0,0,null);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.CYAN);
        canvas.drawCircle(x*2, y*2, 20, paint);
        bmFrame = bitmap;
        ListOfPoints[PointsDefined].x = (x*2/img.getWidth());
        ListOfPoints[PointsDefined].y = (y*2/img.getHeight());
        PointsDefined++;
        return bitmap;
    }



    FirebaseDatabase database;
    DatabaseReference myRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("video");
        mStorageRef = FirebaseStorage.getInstance().getReference();
        for(int i = 0; i<5;i++){
            ListOfPoints[i] = new PointF(0,0);
        }
        bReset = (Button) findViewById(R.id.reset);
        bReset.setEnabled(false);
        bUpload = (Button) findViewById(R.id.upload);
        bUpload.setEnabled(false);
        final Intent startvideoView = new Intent(this, ViewVideo.class);
        // Read from the database
        myRef = database.getReference("video");
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                String Avalue = dataSnapshot.child("analyzed").getValue().toString();
                String Uvalue = dataSnapshot.child("vidUploaded").getValue().toString();
                String Pvalue = dataSnapshot.child("percentageDone").getValue().toString();
                Log.d("App", "Vannolue is: ." + Avalue+".");
                if(Avalue.equals("True")){
                    myRef.child("analyzed").setValue("False");
                    Log.d("App", "Value is: ." + Avalue+".");
                    Intent i = new Intent(startvideoView);
                    startActivity(i);
                }
                if(Uvalue.equals("True")){
                    TextView t = findViewById(R.id.textView2);
                    t.setText("Percentage Completed: "+Pvalue);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w("App", "Failed to read value.", error.toException());
            }
        });
    }


    public void dispatchTakeVideoIntent(View view) {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        }
    }

    public void resetPoints(View view){
        bReset.setEnabled(false);
        bUpload.setEnabled(false);
        bmFrame = backUpImage;
        PointsDefined = 0;
        capturedImageView.setImageBitmap(backUpImage);
    }

    public void upload(View view){
        final String filename = file.toString().replace("/","").replace(":","");
        StorageReference riversRef = mStorageRef.child(filename);
        riversRef.putFile(file)
            .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    myRef.child("analyzed").setValue("False");
                    myRef.child("vidUploaded").setValue("True");
                    myRef.child("filename").setValue(filename);
                    myRef.child("percentageDone").setValue(0);
                    for(int i = 0;i<5;i++){
                        myRef.child("point"+i+"XVal").setValue(ListOfPoints[i].x);
                        myRef.child("point"+i+"YVal").setValue(ListOfPoints[i].y);
                    }
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle unsuccessful uploads
                    // ...
                }
            });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            file = intent.getData();
            capturedImageView = (ImageView)findViewById(R.id.imageView);
            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            Uri videoUri = intent.getData();
            mediaMetadataRetriever.setDataSource(this,videoUri);
            bmFrame = mediaMetadataRetriever.getFrameAtTime(0); //unit in microsecond
            backUpImage = bmFrame;
            capturedImageView.setImageBitmap(bmFrame);
            capturedImageView.setOnTouchListener(touchListener);
        }
    }
}
