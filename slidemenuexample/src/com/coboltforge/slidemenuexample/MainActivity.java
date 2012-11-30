
package com.coboltforge.slidemenuexample;

import com.coboltforge.slidemenu.SlideMenu;
import com.coboltforge.slidemenu.SlideMenuInterface.OnSlideMenuItemClickListener;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity implements OnSlideMenuItemClickListener {

	private ArrayAdapter<String> adapter;
	private SlideMenu slidemenu;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		
		/*
		 * There are two ways to add the slide menu: 
		 * From code or to inflate it from XML (then you have to declare it in the activities layout XML)
		 */
		// this is from code. no XML declaration necessary, but you won't get state restored after rotation.
//		slidemenu = new SlideMenu(this);
		// this inflates the menu from XML. open/closed state will be restored after rotation, but you'll have to call init.
		slidemenu = (SlideMenu) findViewById(R.id.slideMenu);
		slidemenu.init(this);
		
		String[] items = new String[]{"Item One", "Item Two", "Won't close", "Item Three"};
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, items);
		slidemenu.setListAdapter(adapter);
		slidemenu.setCallback(this);
		
		// this can set the menu to initially shown instead of hidden
//		slidemenu.setAsShown();
		
		// connect the fallback button in case there is no ActionBar
		Button b = (Button) findViewById(R.id.buttonMenu);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				slidemenu.show();
			}
		});
		
	}


	@Override
	public boolean onSlideMenuItemClick(int position, long itemId) {

		switch(position) {
		case 0:
			Toast.makeText(this, "Item one selected", Toast.LENGTH_SHORT).show();
			break;
		case 1:
			Toast.makeText(this, "Item two selected", Toast.LENGTH_SHORT).show();
			break;
		case 2:
			Toast.makeText(this, "Won't close", Toast.LENGTH_SHORT).show();
			return false;
		case 3:
			Toast.makeText(this, "Item four selected", Toast.LENGTH_SHORT).show();
			break;
		}
		
		return true;
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case android.R.id.home: // this is the app icon of the actionbar
			slidemenu.show();
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	
}