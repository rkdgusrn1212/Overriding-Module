package com.khgkjg12.overriding.overridingmodule;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;


/**
 * 권한 설명용.
 * setListener 를 통한 콜백 함수 등록 필요. 미등록시 예외처리
 */
public class PermissionExplainDialog extends DialogFragment {

    private String mPermissionName;
    private String mText;
    private OnResultListener mListener;

    public static PermissionExplainDialog newInstance(String permissionName, String text, @NonNull OnResultListener listener) {
        PermissionExplainDialog f = new PermissionExplainDialog();

        if(listener == null){
            throw new RuntimeException("No Listener on permissionExplainDialog");
        }
        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putString("permission", permissionName);
        args.putString("text",text);
        f.setArguments(args);
        f.setOnResultListener(listener);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        mPermissionName = bundle.getString("permission");
        mText = bundle.getString("text");
        setStyle(DialogFragment.STYLE_NORMAL, 0);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("권한 요청");
        builder.setMessage("해당 서비스를 사용하려면 \""+mPermissionName+"\" 권한이 필요합니다.\n"+mText)
                .setPositiveButton("허가", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if(mListener!=null) {
                            mListener.agreeToPermissionExplainDialog();
                        }else{
                            throw new RuntimeException("No Listener on permissionExplainDialog");
                        }
                    }
                })
                .setNegativeButton("거절", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }


    private void setOnResultListener(OnResultListener listener){
        mListener = listener;
    }

    public interface OnResultListener{
        void agreeToPermissionExplainDialog();
    }
}
