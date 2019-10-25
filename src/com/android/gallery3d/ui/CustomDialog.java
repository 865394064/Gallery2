package com.android.gallery3d.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.gallery3d.R;
import android.os.Bundle;
import android.app.AlertDialog;



public class CustomDialog extends Dialog {
      public CustomDialog(Context context) {
		super(context);
	}

	public CustomDialog(Context context, int theme) {
		super(context, theme);
	}
    public static class Builder {
		private Context context;
		private String title;
		private String message;
		private String positiveButtonText;
		private String negativeButtonText;
		private View contentView;
		private String info;
		private DialogInterface.OnClickListener positiveButtonClickListener;
		private DialogInterface.OnClickListener negativeButtonClickListener;
		View layout;
    	
	public Builder(Context context) {
			this.context = context;
		}

	public Builder setMessage(String message) {
			this.message = message;
			return this;
		}

	public Builder setMessage(int message) {
			this.message = (String) context.getText(message);
			return this;
		}

	public Builder setInfo(String message) {
			this.info = message;
			return this;
		}
	public Builder setInfo(int message) {
			this.info = (String) context.getText(message);
			return this;
		}
	public Builder setTitle(int title) {
			this.title = (String) context.getText(title);
			return this;
		}
	public Builder setTitle(String title) {
			this.title = title;
			return this;
		}

     public Builder setContentView(View v) {
			this.contentView = v;
			return this;
		}

   public Builder setPositiveButton(int positiveButtonText,
				DialogInterface.OnClickListener listener) {
			this.positiveButtonText = (String) context
					.getText(positiveButtonText);
			this.positiveButtonClickListener = listener;
			return this;
		}

		public Builder setPositiveButton(String positiveButtonText,
				DialogInterface.OnClickListener listener) {
			this.positiveButtonText = positiveButtonText;
			this.positiveButtonClickListener = listener;
			return this;
		}

		public Builder setNegativeButton(int negativeButtonText,
				DialogInterface.OnClickListener listener) {
			this.negativeButtonText = (String) context
					.getText(negativeButtonText);
			this.negativeButtonClickListener = listener;
			return this;
		}

		public Builder setNegativeButton(String negativeButtonText,
				DialogInterface.OnClickListener listener) {
			this.negativeButtonText = negativeButtonText;
			this.negativeButtonClickListener = listener;
			return this;
		}	 
	
   public   CustomDialog create(){

		LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final CustomDialog dialog = new CustomDialog(context,
					R.style.Dialog);
		layout = inflater.inflate(R.layout.dialog_normal_layout, null);
		dialog.addContentView(layout, new LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		((TextView) layout.findViewById(R.id.title)).setText(title);
         //  AlertDialog.Builder builder = new AlertDialog.Builder(context);        
            // LayoutInflater factory= LayoutInflater.from(context); 
        //    final View textEntryView = factory.inflate(R.layout.dialog_normal_layout, null);
           // builder.setIcon(R.drawable.ic_launcher);
          //  builder.setTitle(R.string.new_gallery_video);
          //  builder.setView(textEntryView);
          /*  dialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                
                }
            }); */
            if (positiveButtonText != null) {
				((Button) layout.findViewById(R.id.positiveButton))
						.setText(positiveButtonText);
				if (positiveButtonClickListener != null) {
					((Button) layout.findViewById(R.id.positiveButton))
							.setOnClickListener(new View.OnClickListener() {
								public void onClick(View v) {
									positiveButtonClickListener.onClick(dialog,
											DialogInterface.BUTTON_POSITIVE);
								}
							});
				}
			} else {
				// if no confirm button just set the visibility to GONE
				layout.findViewById(R.id.positiveButton).setVisibility(
						View.GONE);
			}
         /*   dialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                }
            });*/
            if (negativeButtonText != null) {
				((Button) layout.findViewById(R.id.negativeButton))
						.setText(negativeButtonText);
				if (negativeButtonClickListener != null) {
					((Button) layout.findViewById(R.id.negativeButton))
							.setOnClickListener(new View.OnClickListener() {
								public void onClick(View v) {
									negativeButtonClickListener.onClick(dialog,
											DialogInterface.BUTTON_NEGATIVE);
								}
							});
				}
			} else {
				// if no confirm button just set the visibility to GONE
				layout.findViewById(R.id.negativeButton).setVisibility(
						View.GONE);
			}
			if (message != null) {
				((TextView) layout.findViewById(R.id.message)).setText(message);
				if (info != null) {
					((EditText) layout.findViewById(R.id.input_text))
							.setText(info);
					((EditText) layout.findViewById(R.id.input_text)).setSelection(info.length());
				}
			} else if (contentView != null) {
				// if no message set
				// add the contentView to the dialog body
				((LinearLayout) layout.findViewById(R.id.message))
						.removeAllViews();
				((LinearLayout) layout.findViewById(R.id.message)).addView(
						contentView, new LayoutParams(
								LayoutParams.WRAP_CONTENT,
								LayoutParams.WRAP_CONTENT));
			}

			dialog.setContentView(layout);
                     return dialog;
			//dialog.show();
           	   
		}
   public void setVisibilityOfInputTextView(int visibility) {
			if (layout != null) {
				((EditText) layout.findViewById(R.id.input_text)).requestFocus();
				((EditText) layout.findViewById(R.id.input_text)).setVisibility(visibility);
			}
		}
		public EditText getInputTextView(){
                    if (layout != null) {
				EditText mEditText = ((EditText) layout.findViewById(R.id.input_text));
				return mEditText;
			}else{
                            return null;
			}
		}
		public String getInputText(){
			if(info!=null){
				return ((EditText) layout.findViewById(R.id.input_text)).getText().toString();
			}else{
				return "error";
			}
                 }
		}
    	}

