package com.platypus.android.tablet;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Handler;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.platypus.crw.data.SensorData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by jason on 10/26/17.
 * Display unique sensors data as text at the bottom of the main gui activity
 * https://stackoverflow.com/questions/28531996/android-recyclerview-gridlayoutmanager-column-spacing
 * https://stackoverflow.com/questions/32457406/how-to-update-refresh-specific-item-in-recyclerview
 */

public class SensorStuff
{
		// each boat has its own list of unique sensors
		// when you switch boats, the recycler view needs to start referencing the new boat's list
		private List<SensorData> data_list = new ArrayList<>();
		private HashMap<Integer, Runnable> unique_sensors_runnable_map = new HashMap<>();
		Handler item_remover_handler = new Handler();
		long item_removal_delay_ms = 10000;
		String logTag = "SensorStuff";
		final Object data_list_lock = new Object();

		Activity main_gui_activity;
		private RecyclerView mRecyclerView;
		private RecyclerView.Adapter mAdapter;
		private RecyclerView.LayoutManager mLayoutManager;

		SensorStuff(Activity activity)
		{
				main_gui_activity = activity;
				mRecyclerView = (RecyclerView) main_gui_activity.findViewById(R.id.sensor_recycler_view);
				mAdapter = new SensorDataTextAdapter();
				mRecyclerView.setAdapter(mAdapter);
				int spanCount = 4; // number of columns
				int spacing = 20;
				boolean includeEdge = true;
				mLayoutManager = new GridLayoutManager(main_gui_activity.getApplicationContext(), spanCount);
				mRecyclerView.setLayoutManager(mLayoutManager);
				mRecyclerView.addItemDecoration(new GridSpacingItemDecoration(spanCount, spacing, includeEdge));
		}

		class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

				private int spanCount;
				private int spacing;
				private boolean includeEdge;

				public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
						this.spanCount = spanCount;
						this.spacing = spacing;
						this.includeEdge = includeEdge;
				}

				@Override
				public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
						int position = parent.getChildAdapterPosition(view); // item position
						int column = position % spanCount; // item column

						if (includeEdge) {
								outRect.left = spacing - column * spacing / spanCount; // spacing - column * ((1f / spanCount) * spacing)
								outRect.right = (column + 1) * spacing / spanCount; // (column + 1) * ((1f / spanCount) * spacing)

								if (position < spanCount) { // top edge
										outRect.top = spacing;
								}
								outRect.bottom = spacing; // item bottom
						} else {
								outRect.left = column * spacing / spanCount; // column * ((1f / spanCount) * spacing)
								outRect.right = spacing - (column + 1) * spacing / spanCount; // spacing - (column + 1) * ((1f /    spanCount) * spacing)
								if (position >= spanCount) {
										outRect.top = spacing; // item top
								}
						}
				}
		}

		class SensorDataTextAdapter extends RecyclerView.Adapter<SensorDataTextAdapter.SensorDataHolder>
		{
				class SensorDataHolder extends RecyclerView.ViewHolder
				{
						View view;
						SensorData sd;
						SensorDataHolder(View v)
						{
								super(v);
								view = v;
						}
						void bindSensorData(SensorData _sd) { sd = _sd; }
				}

				// Create new views (invoked by the layout manager)
				@Override
				public SensorDataTextAdapter.SensorDataHolder onCreateViewHolder(ViewGroup parent,
				                                                    int viewType) {
						// create a new view
						View v = LayoutInflater.from(parent.getContext())
										.inflate(R.layout.sensor_text_display_layout, parent, false);

						// set the view's size, margins, paddings and layout parameters
						SensorDataHolder vh = new SensorDataHolder(v);
						return vh;
				}

				// Replace the contents of a view (invoked by the layout manager)
				@Override
				public void onBindViewHolder(SensorDataHolder holder, int position)
				{
						// - get element from your dataset at this position
						// - replace the contents of the view with that element
						TextView value = (TextView)holder.view.findViewById(R.id.sensor_text_display_VALUE);
						TextView type = (TextView)holder.view.findViewById(R.id.sensor_text_display_TYPE);
						TextView units = (TextView)holder.view.findViewById(R.id.sensor_text_display_UNITS);
						synchronized (data_list_lock)
						{
								SensorData sd = data_list.get(position);
								holder.bindSensorData(sd);
								value.setText(Double.toString(sd.value));
								type.setText(sd.type.getType());
								units.setText(sd.type.getUnits());

								// post a delayed runnable that will remove the text once it is old enough
								RemoveOldItemRunnable new_runnable = new RemoveOldItemRunnable(sd.key());
								if (unique_sensors_runnable_map.containsKey(sd.key()))
								{
										Log.d(logTag, String.format("Replacing runnable for position %d", position));
										// need to replace old runnable, so remove it before posting the new one again
										Runnable old_runnable = unique_sensors_runnable_map.get(sd.key());
										if (old_runnable != null) item_remover_handler.removeCallbacks(old_runnable);
								}
								else
								{
										Log.d(logTag, String.format("Starting new runnable for position %d", position));
								}
								item_remover_handler.postDelayed(new_runnable, item_removal_delay_ms);
								unique_sensors_runnable_map.put(sd.key(), new_runnable);
						}
				}

				// Return the size of your dataset (invoked by the layout manager)
				@Override
				public int getItemCount()
				{
						synchronized (data_list_lock)
						{
								return data_list.size();
						}
				}
		}

		class RemoveOldItemRunnable implements Runnable
		{
				int key;
				RemoveOldItemRunnable(int _key)
				{
						key = _key;
				}
				@Override
				public void run()
				{
						synchronized (data_list_lock)
						{
								// position can change, but can be determined with key
								// need to update position at the moment this runs by matching the key
								for (int i = 0; i < data_list.size(); i++)
								{
										if (data_list.get(i).key() == key)
										{
												Log.d(logTag, String.format("Removing SensorData item at position %d", i));
												unique_sensors_runnable_map.remove(key);
												data_list.remove(i);
												mAdapter.notifyItemRemoved(i);
												mAdapter.notifyDataSetChanged(); // stops the buggy growth of the recycler view box when this runnable executes
												return;
										}
								}
						}
				}
		}

		void newSensorSet()
		{
				synchronized (data_list_lock)
				{
						// switching boats, so clear out everything
						mAdapter.notifyItemRangeRemoved(0, mAdapter.getItemCount());
						for (Map.Entry<Integer, Runnable> entry : unique_sensors_runnable_map.entrySet())
						{
								item_remover_handler.removeCallbacks(entry.getValue());
						}
						unique_sensors_runnable_map.clear();
						data_list.clear();
				}
		}

		void newSensorData(SensorData sd)
		{
				synchronized (data_list_lock)
				{
						Log.i(logTag, String.format("Submitted new SensorData for display: %s", sd.toString()));
						// use sd.key()
						// if key is new and unique, bind a new item
						// if key already exists, find the position of that item and update that item
						Set<Integer> unique_sensors_set = unique_sensors_runnable_map.keySet();
						if (unique_sensors_set.contains(sd.key()))
						{
								Log.i(logTag, "Unique sensor already exists, updating existing item");
								// already exists, update item
								// first, find item in the data_list with the same key
								// then change the item in data_list, notify the adapter you changed it
								for (int i = 0; i < data_list.size(); i++)
								{
										if (data_list.get(i).isSameProbe(sd))
										{
												data_list.set(i, sd);
												mAdapter.notifyItemChanged(i);
												return;
										}
								}
						}
						else
						{
								Log.i(logTag, "New unique sensor, adding new item");
								data_list.add(sd);
								unique_sensors_runnable_map.put(sd.key(), null); // need this here to prevent asynchronous duplicates
								mAdapter.notifyItemInserted(data_list.size() - 1);
						}
				}
		}
}