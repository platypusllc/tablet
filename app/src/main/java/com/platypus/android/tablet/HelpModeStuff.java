package com.platypus.android.tablet;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;

import java.util.HashMap;

import tourguide.tourguide.Overlay;
import tourguide.tourguide.Pointer;
import tourguide.tourguide.ToolTip;
import tourguide.tourguide.TourGuide;

/**
 * Created by jason on 3/30/18.
 */

class HelpModeStuff
{

		Activity main_gui_activity;
		boolean help_mode_on = false;
		boolean isHelpModeOn()
		{
				return help_mode_on;
		}
		void setHelpModeOn(boolean _help_mode_on)
		{
				help_mode_on = _help_mode_on;
		}
		TourGuide getTourGuide(String key)
		{
				return tourguide_map.get(key);
		}

		HashMap<String, TourGuide> tourguide_map = new HashMap<>();
		HashMap<String, Pointer> pointer_map = new HashMap<>();
		HashMap<String, ToolTip> tooltip_map = new HashMap<>();
		HashMap<String, Overlay> overlay_map = new HashMap<>();

		void createTourGuide(final String id, String title, String description, int gravity, int overlay_radius)
		{
				tooltip_map.put(id,
						new ToolTip()
						.setTitle(title)
						.setDescription(description)
						.setGravity(gravity)
						.setShadow(true)
				);
				tooltip_map.get(id).setOnClickListener(new View.OnClickListener()
				{
						@Override
						public void onClick(View v)
						{
								tourguide_map.get(id).cleanUp();
						}
				});
				overlay_map.put(id,
						new Overlay()
								.disableClick(true)
								.disableClickThroughHole(true)
								.setHoleRadius(overlay_radius)
				);
				overlay_map.get(id).setOnClickListener(new View.OnClickListener()
				{
						@Override
						public void onClick(View v)
						{
								tourguide_map.get(id).cleanUp();
						}
				});
				tourguide_map.put(id,
						TourGuide.init(main_gui_activity).with(TourGuide.Technique.CLICK)
										.setPointer(new Pointer())
										.setToolTip(tooltip_map.get(id))
										.setOverlay(overlay_map.get(id))
				);
		}


		HelpModeStuff(Activity activity)
		{
				main_gui_activity = activity;

				createTourGuide(
								"connect_to_boat",
								"Connecting to a boat",
								"This button opens a dialog that...",
								Gravity.RIGHT | Gravity.BOTTOM,
								75
				);

				createTourGuide(
								"center_view",
								"Center the map over the boat",
								"Moves the map view to the GPS location of the currently selected boat",
								Gravity.RIGHT | Gravity.BOTTOM,
								50
				);

				

		}
}
