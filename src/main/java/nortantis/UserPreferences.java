package nortantis;

import java.util.prefs.Preferences;


public class UserPreferences
{
	public String lastLoadedSettingsFile;
	public String lastEditorTool;
	public String zoomLevel;
	public boolean hideMapChangesWarning;
	public boolean hideAspectRatioWarning;
	public boolean hideHeightMapWithEditsWarning;
	
	public static UserPreferences instance;
	
	public static UserPreferences getInstance()
	{
		if (instance == null)
		{
			instance = new UserPreferences();
		}
		return instance;
	}
	
	private UserPreferences()
	{
		Preferences userPreferences = Preferences.userNodeForPackage(getClass());
		lastLoadedSettingsFile = userPreferences.get("lastLoadedSettingsFile", "");
		lastEditorTool = userPreferences.get("lastEditTool", "");
		zoomLevel = userPreferences.get("zoomLevel", "");
		hideMapChangesWarning = userPreferences.getBoolean("hideMapChangesWarning", false);
		hideAspectRatioWarning = userPreferences.getBoolean("hideAspectRatioWarning", false);
		hideHeightMapWithEditsWarning = userPreferences.getBoolean("hideHeightMapWithEditsWarning", false);
	}
	
	public void save()
	{
		Preferences userPreferences = Preferences.userNodeForPackage(getClass());
		userPreferences.put("lastLoadedSettingsFile", lastLoadedSettingsFile);
		userPreferences.put("lastEditTool", lastEditorTool);
		userPreferences.put("zoomLevel", zoomLevel);
		userPreferences.putBoolean("hideMapChangesWarning", hideMapChangesWarning);
		userPreferences.putBoolean("hideAspectRatioWarning", hideAspectRatioWarning);
		userPreferences.putBoolean("hideHeightMapWithEditsWarning", hideHeightMapWithEditsWarning);
	}
}
