package treimers.net.jtimesheet.storage;

import java.util.ArrayList;
import java.util.List;

public class StorageData {
    public List<CustomerData> customers = new ArrayList<>();
    public List<ActivityData> activities = new ArrayList<>();
    public SettingsData settings = new SettingsData();
}
