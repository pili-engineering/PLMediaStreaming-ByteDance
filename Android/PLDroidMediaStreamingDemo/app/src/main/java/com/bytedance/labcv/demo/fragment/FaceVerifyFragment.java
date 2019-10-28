package com.bytedance.labcv.demo.fragment;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bytedance.labcv.demo.MainActivity;
import com.bytedance.labcv.demo.base.IPresenter;
import com.bytedance.labcv.demo.utils.BitmapUtils;
import com.bytedance.labcv.demo.utils.CommonUtils;
import com.bytedance.labcv.demo.utils.ToasUtils;
import com.bytedance.labcv.demo.view.ButtonView;
import com.qiniu.pili.droid.streaming.demo.R;
import com.qiniu.pili.droid.streaming.effect.OnCloseListener;

import static android.app.Activity.RESULT_OK;

public class FaceVerifyFragment
        extends BaseFeatureFragment<IPresenter, FaceVerifyFragment.IFaceVerifyCallback>
        implements View.OnClickListener, OnCloseListener {
    private int REQUEST_ALBUM = 1001;

    private ButtonView bvNone;
    private ButtonView bvUpload;


    public interface IFaceVerifyCallback {
        // 人脸检测
        void onPicChoose(Bitmap bitmap);

        void faceVerifyOn(boolean flag);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return LayoutInflater.from(getActivity()).inflate(R.layout.fragment_face_verify, container, false);
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bvNone = view.findViewById(R.id.bv_none_face_verify);
        bvUpload = view.findViewById(R.id.bv_upload_face_verify);

        bvNone.setOnClickListener(this);
        bvUpload.setOnClickListener(this);

        bvNone.on();
    }

    @Override
    public void onClick(View v) {
        if (CommonUtils.isFastClick()) {
            ToasUtils.show("too fast click");
            return;
        }
        switch (v.getId()) {
            case R.id.bv_none_face_verify:
                boolean isVerifyOn = bvNone.isOn();

                getCallback().faceVerifyOn(isVerifyOn);
                bvNone.change(!isVerifyOn);
                bvUpload.change(isVerifyOn);
                break;
            case R.id.bv_upload_face_verify:
                if (!bvNone.isOn()) {
                    chooseImg();
                }
                break;
        }
    }

    private void chooseImg() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (intent.resolveActivity(getActivity().getPackageManager())!= null){
            startActivityForResult(intent, REQUEST_ALBUM);
        } else {
            ToasUtils.show(getString(R.string.ablum_not_supported));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ALBUM && resultCode == RESULT_OK && null != data) {
            if (Build.VERSION.SDK_INT >= 19) {
                handleImageOnKitkat(data);
            } else {
                handleImageBeforeKitkat(data);
            }
        }
    }

    @Override
    public void onClose() {
        getCallback().faceVerifyOn(false);

        bvNone.on();
        bvUpload.off();
    }

    @TargetApi(19)
    private void handleImageOnKitkat(Intent data) {
        String imagePath = null;
        Uri uri = data.getData();
        String scheme = uri.getScheme();
        if (DocumentsContract.isDocumentUri(getActivity(), uri)) {
            String docId = DocumentsContract.getDocumentId(uri);
            String authority = uri.getAuthority();
            if ("com.android.providers.media.documents".equals(authority)) {
                String id = docId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(authority)) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content:" +
                        "//downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(scheme)) {
            imagePath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(scheme)) {
            imagePath = uri.getPath();
        }
        displayImage(imagePath);

    }

    private void handleImageBeforeKitkat(Intent data) {
        Uri uri = data.getData();
        String imagePath = getImagePath(uri, null);
        displayImage(imagePath);

    }

    private String getImagePath(Uri uri, String selection) {
        String path = null;
        Cursor cursor = getActivity().getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    private void displayImage(String imagePath) {
        Bitmap bitmap = BitmapUtils.decodeBitmapFromFile(imagePath, 800, 800);
        if (bitmap != null && !bitmap.isRecycled()) {
            if (null != getCallback()) {
                getCallback().onPicChoose(bitmap);
            }
        } else {
            ToasUtils.show("failed to get image");
        }
    }
}
