package com.platypus.android.tablet;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jason on 10/13/17.
 * https://www.raywenderlich.com/170075/android-recyclerview-tutorial-kotlin
 */

public class AutonomyActivity extends Activity
{
		private static TeleOpPanel tpanel;
		public static final void set_TeleOpPanel(TeleOpPanel tpanel_) { tpanel = tpanel_;}

		private RecyclerView mRecyclerView;
		private RecyclerView.Adapter mAdapter;
		private RecyclerView.LayoutManager mLayoutManager;

		static List<AutonomousPredicateMessage> apm_list = new ArrayList<>();

		@Override
		protected void onCreate(Bundle savedInstanceState)
		{
				super.onCreate(savedInstanceState);

				setContentView(R.layout.autonomous_control_panel);
				mRecyclerView = (RecyclerView) findViewById(R.id.autonomy_recycler_view);

				// use a linear layout manager
				mLayoutManager = new LinearLayoutManager(this);
				mRecyclerView.setLayoutManager(mLayoutManager);

				// specify an adapter (see also next example)
				apm_list.add(new AutonomousPredicateMessage("1", "action", "trigger", 1000, true));
				apm_list.add(new AutonomousPredicateMessage("2", "action", "trigger", 1000, true));
				mAdapter = new AutonomyAdapter(apm_list);
				mRecyclerView.setAdapter(mAdapter);

				Button create_new_apm_button = (Button)findViewById(R.id.create_new_apm_button);
				create_new_apm_button.setOnClickListener(new View.OnClickListener()
				{
						@Override
						public void onClick(View v)
						{
								apm_list.add(new AutonomousPredicateMessage(
												Integer.toString(apm_list.size()+1), "action", "trigger", 1000, true));
								mAdapter.notifyItemInserted(apm_list.size()-1);
						}
				});
				// TODO: mAdapter.notifyItemChanged(int position);
		}


		class AutonomyAdapter extends RecyclerView.Adapter<AutonomyAdapter.APMHolder> {

				List<AutonomousPredicateMessage> adapter_apm_list;

				// Provide a suitable constructor (depends on the kind of dataset)
				public AutonomyAdapter(List<AutonomousPredicateMessage> list)
				{
						adapter_apm_list = list;
				}

				// Provide a reference to the views for each data item
				// Complex data items may need more than one view per item, and
				// you provide access to all the views for a data item in a view holder
				class APMHolder extends RecyclerView.ViewHolder
				{
						// each data item represents an autonomous predicate message
						public View view;

						AutonomousPredicateMessage apm;

						public APMHolder(View v)
						{
								super(v);
								view = v;
						}

						void bindAPM(AutonomousPredicateMessage _apm)
						{
								apm = _apm;
						}
				}

				// Create new views (invoked by the layout manager)
				@Override
				public AutonomyAdapter.APMHolder onCreateViewHolder(ViewGroup parent,
				                                               int viewType) {
						// create a new view
						View v = LayoutInflater.from(parent.getContext())
										.inflate(R.layout.auto_predicate_layout, parent, false);

						// set the view's size, margins, paddings and layout parameters
						APMHolder vh = new APMHolder(v);
						return vh;
				}

				// Replace the contents of a view (invoked by the layout manager)
				@Override
				public void onBindViewHolder(APMHolder holder, int position)
				{
						// - get element from your dataset at this position
						// - replace the contents of the view with that element
						TextView name = (TextView)holder.view.findViewById(R.id.apm_name);
						TextView trigger = (TextView)holder.view.findViewById(R.id.apm_trigger);
						holder.bindAPM(adapter_apm_list.get(position));
						name.setText(String.format("name: %s", holder.apm.getName()));
						trigger.setText(String.format("trigger: %s", holder.apm.getTrigger()));
				}

				// Return the size of your dataset (invoked by the layout manager)
				@Override
				public int getItemCount()
				{
						return adapter_apm_list.size();
				}
		}
}
