package at.jbiering.mediatransmitter;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import at.jbiering.mediatransmitter.model.Device;
import at.jbiering.mediatransmitter.websocket.BaseWebSocketActivity;

public class ChatActivity extends BaseWebSocketActivity {

    private int requestCode = 200;

    private Button buttonAddFile;
    private Button buttonSendMessage;
    private EditText editTextMessage;
    private TextView textViewPersonName;
    private TextView textViewIp;
    private Device otherClientsDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        if(getIntent() != null && getIntent().hasExtra("device")){
            Device device = getIntent().getParcelableExtra("device");
            otherClientsDevice = device;
        }

        findViews();
        setTextViewTexts();
        setButtonOnClickListeners();
        //disable all views for interacting with other clients as long as ws service
        //has not been bounded as interaction is not possible
        setInteractionViewsEnabled(false);
    }

    @Override
    protected void onWebsocketServiceBounded() {
        super.onWebsocketServiceBounded();
        //websocket service is bounded now -> interaction possible -> enable all disabled views
        setInteractionViewsEnabled(true);
    }

    private void setInteractionViewsEnabled(boolean enabled) {
        this.editTextMessage.setEnabled(enabled);
        this.buttonAddFile.setEnabled(enabled);
        this.buttonSendMessage.setEnabled(enabled);
    }

    private void setButtonOnClickListeners() {
        buttonSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String messageText = editTextMessage.getText().toString();
                webSocketService.sendTextMessage(messageText);
            }
        });

        buttonAddFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent()
                        .setType("*/*")
                        .setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent
                        .createChooser(intent, "Select a file"), requestCode);
            }
        });
    }

    private void setTextViewTexts() {
        textViewPersonName.setText(otherClientsDevice.getName());
        textViewIp.setText(otherClientsDevice.getIp());
    }

    private void findViews() {
        buttonAddFile = findViewById(R.id.buttonAddFile);
        buttonSendMessage = findViewById(R.id.buttonSendMessage);
        editTextMessage = findViewById(R.id.editTextMessage);
        textViewPersonName = findViewById(R.id.textViewPersonName);
        textViewIp = findViewById(R.id.textViewIp);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == this.requestCode && resultCode == RESULT_OK){
            Uri selectedFile = data.getData();
            String fileExtension = MimeTypeMap
                    .getSingleton()
                    .getExtensionFromMimeType(getContentResolver().getType(selectedFile));
            webSocketService
                    .sendMediaFile(fileExtension,
                            selectedFile, otherClientsDevice.getId());
        }
    }
}
