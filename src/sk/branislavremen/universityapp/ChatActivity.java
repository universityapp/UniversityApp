package sk.branislavremen.universityapp;

import java.util.ArrayList;
import java.util.List;

import sk.branislavremen.universityapp.adapter.ChatListAdapter;
import sk.branislavremen.universityapp.vo.MessageData;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.LogInCallback;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class ChatActivity extends Activity {

	ParseUser currentUser;
	String currentRoom;

	String role = "visitor";
	Boolean isTeacherConfirmed = false;
	
	String lastMsg;

	private EditText etMessage;
	private Button btSend;

	private ListView lvChat;
	private ArrayList<MessageData> mMessages;
	private ChatListAdapter mAdapter;

	private static final int MAX_CHAT_MESSAGES_TO_SHOW = 30;

	// Create a handler which can run code periodically
	private Handler handler = new Handler();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);

		startWithCurrentUser();

		handler.postDelayed(runnable, 500);
	}

	// Defines a runnable which is run every 100ms
	private Runnable runnable = new Runnable() {
		@Override
		public void run() {
			refreshMessages();
			handler.postDelayed(this, 15000);
		}
	};

	private void refreshMessages() {
		receiveMessage();
	}

	// Get the userId from the cached currentUser object
	private void startWithCurrentUser() {
		
		currentUser = ParseUser.getCurrentUser();

		currentUser.fetchInBackground(new GetCallback<ParseUser>() {

			@Override
			public void done(ParseUser object, ParseException e) {
				// TODO Auto-generated method stub
				role = object.getString("Role");

				isTeacherConfirmed = object.getBoolean("teacherConfirmation");

				if (role.equalsIgnoreCase("visitor")) {
					finish();
					// toast
				}

				if (role.equalsIgnoreCase("student")) {
					String studyProgramme = object.getString("StudyProgramme");
					String rocnik = object.getString("Rocnik");
					if (studyProgramme == null || rocnik == null) {
						finish();
						// Toast
					} else {
						currentRoom = studyProgramme + " (" + rocnik + ")";
						Log.d("chat", "student in room " + currentRoom);

					}

				}

				if (role.equalsIgnoreCase("teacher")) {

					if (isTeacherConfirmed) {

					} else {
						finish();
						// toast
					}
				}

				if (role.equalsIgnoreCase("admin")) {

				}

				setupMessagePosting();

			}
		});

	}

	// Setup message field and posting
	private void setupMessagePosting() {
		etMessage = (EditText) findViewById(R.id.etMessage);
		btSend = (Button) findViewById(R.id.btSend);
		lvChat = (ListView) findViewById(R.id.lvChat);
		mMessages = new ArrayList<MessageData>();
		mAdapter = new ChatListAdapter(ChatActivity.this, currentUser.getObjectId(),  mMessages);
		lvChat.setAdapter(mAdapter);
		btSend.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String body = etMessage.getText().toString();

				// Use Message model to create new messages now
				MessageData message = new MessageData();
				message.setUserId(currentUser.getObjectId());
				message.setBody(body);
				message.setRoom(currentRoom);
				message.saveInBackground(new SaveCallback() {
					@Override
					public void done(ParseException e) {
						receiveMessage();
					}
				});
				etMessage.setText("");
			}
		});
	}

	// Query messages from Parse so we can load them into the chat adapter
	private void receiveMessage() {
		// Construct query to execute
		ParseQuery<MessageData> query = ParseQuery.getQuery(MessageData.class);
		// Configure limit and sort order
		query.setLimit(MAX_CHAT_MESSAGES_TO_SHOW);
		query.orderByAscending("createdAt");
		query.whereEqualTo("room", currentRoom);
		// Execute query to fetch all messages from Parse asynchronously
		// This is equivalent to a SELECT query with SQL
		query.findInBackground(new FindCallback<MessageData>() {
			public void done(List<MessageData> messages, ParseException e) {
				if (e == null) {
					mMessages.clear();
					mMessages.addAll(messages);
					mAdapter.notifyDataSetChanged(); // update adapter
					lvChat.invalidate(); // redraw listview
				} else {
					// Log.d("message", "Error: " + e.getMessage());
				}
			}
		});
	}

}
