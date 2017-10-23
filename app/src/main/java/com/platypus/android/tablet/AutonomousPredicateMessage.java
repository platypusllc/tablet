package com.platypus.android.tablet;

import android.util.Log;

import org.json.JSONObject;

/**
 * Created by jason on 9/11/17.
 */

public class AutonomousPredicateMessage
{
		private JSONObject json = null;
		private String name;
		private String action_string = "do_nothing";
		private String trigger_string = "always_true";
		private long interval = 1000;
		private boolean ends = true;
		private String stringified_json = null;
		private boolean sent_and_acknowledged = false;
		AutonomousPredicateMessage(String _name,
		                           String action,
		                           String trigger,
		                           long _interval,
		                           boolean _ends)
		{
				name = _name;
				action_string = action;
				trigger_string = trigger;
				interval = _interval;
				ends = _ends;
		}
		public JSONObject generateJSON()
		{
				try
				{
						json = new JSONObject();
						json.put(name,
										new JSONObject()
														.put("a", action_string)
														.put("t", trigger_string)
														.put("i", interval)
														.put("e", ends));
				}
				catch (Exception e)
				{
						Log.e("AutonomousPredicate",
										String.format("AutonomousPredicateMessage generateJSON error: %s",
														e.getMessage()));
						json = null;
				}
				return json;
		}
		public String generateStringifiedJSON()
		{
				if (generateJSON() != null)
				{
						stringified_json = json.toString();
						return stringified_json;
				}
				else
				{
						return null;
				}
		}
		public boolean getAcknowledged()
		{
				return sent_and_acknowledged;
		}
		public void setAcknowledged(boolean o)
		{
				sent_and_acknowledged = o;
		}
		public String getName() { return name; }
		public String getTrigger() { return trigger_string; }

}
