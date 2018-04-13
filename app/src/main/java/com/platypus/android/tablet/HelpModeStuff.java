package com.platypus.android.tablet;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

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

		private Activity main_gui_activity;
		private boolean help_mode_on = false;
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

		private HashMap<String, TourGuide> tourguide_map = new HashMap<>();
		private HashMap<String, Pointer> pointer_map = new HashMap<>();
		private HashMap<String, ToolTip> tooltip_map = new HashMap<>();
		private HashMap<String, Overlay> overlay_map = new HashMap<>();

		private void createTourGuide(final String id, String title, String description, int gravity, int overlay_radius)
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
								"Connect to a boat with an IP address",
								"",
								Gravity.RIGHT | Gravity.BOTTOM,
								75
				);

				createTourGuide(
								"center_view",
								"Center the map over the boat",
								"",
								Gravity.RIGHT | Gravity.BOTTOM,
								50
				);

				createTourGuide(
								"start_autonomy",
								"Begin autonomous navigation",
								"",
								Gravity.RIGHT | Gravity.BOTTOM,
								40
				);

				createTourGuide(
								"pause_autonomy",
								"Pause/Unpause autonomous navigation",
								"",
								Gravity.RIGHT | Gravity.BOTTOM,
								40
				);

				createTourGuide(
								"stop_autonomy",
								"End autonomous navigation",
								"",
								Gravity.RIGHT | Gravity.BOTTOM,
								40
				);

				createTourGuide(
								"undo_last_wp",
								"Remove last waypoint",
								"",
								Gravity.RIGHT | Gravity.BOTTOM,
								40
				);

				createTourGuide(
								"remove_all_waypoints",
								"Remove all waypoints",
								"",
								Gravity.RIGHT | Gravity.BOTTOM,
								40
				);

				createTourGuide(
								"drop_wp_at_boat",
								"Drops a waypoint at the boat's current position",
								"",
								Gravity.RIGHT | Gravity.BOTTOM,
								40
				);

				createTourGuide(
								"straight_path",
								"Creates path through waypoints",
								"",
								Gravity.RIGHT | Gravity.BOTTOM,
								40
				);

				createTourGuide(
								"spiral",
								"Creates a spiral path covering the area defined by waypoints",
								"Try different transect distances",
								Gravity.RIGHT | Gravity.BOTTOM,
								40
				);

				createTourGuide(
								"lawnmower",
								"Creates an East-West lawnmower pattern covering the area defined by waypoints",
								"Try different transect distances",
								Gravity.RIGHT | Gravity.BOTTOM,
								40
				);

				createTourGuide(
								"reverse_wp_order",
								"Reverses the order of waypoints",
								"",
								Gravity.RIGHT | Gravity.BOTTOM,
								40
				);

				createTourGuide(
								"jar_1",
								"Start filling sampler jar # 1",
								"",
								Gravity.RIGHT | Gravity.TOP,
								40
				);

				createTourGuide(
								"jar_2",
								"Start filling sampler jar # 2",
								"",
								Gravity.RIGHT | Gravity.TOP,
								40
				);

				createTourGuide(
								"jar_3",
								"Start filling sampler jar # 3",
								"",
								Gravity.RIGHT | Gravity.TOP,
								40
				);

				createTourGuide(
								"jar_4",
								"Start filling sampler jar # 4",
								"",
								Gravity.RIGHT | Gravity.TOP,
								40
				);

				createTourGuide(
								"stop_jars",
								"Press and hold to stop all jars",
								"",
								Gravity.RIGHT | Gravity.TOP,
								40
				);

				createTourGuide(
								"reset_sampler",
								"Press and hold to reset sampler jars",
								"",
								Gravity.RIGHT | Gravity.TOP,
								40
				);

				createTourGuide(
								"adv_opts",
								"Expands advanced option menu",
								"",
								Gravity.LEFT | Gravity.BOTTOM,
								75
				);

				createTourGuide(
								"sat_map",
								"Switch to satellite map imagery",
								"",
								Gravity.LEFT,
								75
				);

				// TODO:
				/*

				all advanced options
				 */

		}
}
