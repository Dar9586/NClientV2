package com.dar.nclientv2.components.status;

import androidx.annotation.Nullable;

import com.dar.nclientv2.async.database.Queries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class StatusManager {
    public static final String DEFAULT_STATUS = "None";
    private static HashMap<String, Status> statusMap = new HashMap<>();

    public static Status getByName(String name) {
        return statusMap.get(name);
    }

    public static Status add(String name, int color) {
        return add(new Status(color, name));
    }

    static Status add(Status status) {
        Queries.StatusTable.insert(status);
        statusMap.put(status.name, status);
        return status;
    }

    public static void remove(Status status) {
        Queries.StatusTable.remove(status.name);
        statusMap.remove(status.name);
    }

    public static List<String> getNames() {
        List<String> st = new ArrayList<>(statusMap.keySet());
        Collections.sort(st, String::compareToIgnoreCase);
        st.remove(DEFAULT_STATUS);
        //st.add(0, DEFAULT_STATUS);
        return st;
    }

    public static List<Status> toList() {
        ArrayList<Status> statuses = new ArrayList<>(statusMap.values());
        Collections.sort(statuses, (o1, o2) -> o1.name.compareToIgnoreCase(o2.name));
        statuses.remove(getByName(DEFAULT_STATUS));
        return statuses;
    }

    public static Status updateStatus(@Nullable Status oldStatus, String newName, int newColor) {
        if (oldStatus == null)
            return add(newName, newColor);
        Status newStatus = new Status(newColor, newName);
        Queries.StatusTable.update(oldStatus, newStatus);
        statusMap.remove(oldStatus.name);
        statusMap.put(newStatus.name, newStatus);
        return newStatus;
    }
}
