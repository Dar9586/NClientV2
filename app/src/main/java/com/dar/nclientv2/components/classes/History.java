package com.dar.nclientv2.components.classes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class History{
    private final String value;
    private final Date date;

    public History(String value,boolean set) {
        if(set){
            int p=value.indexOf('|');
            date=new Date(Long.parseLong(value.substring(0,p)));
            this.value=value.substring(p+1);
        }else {
            this.value = value;
            this.date = new Date();
        }
    }
    public static List<History> setToList(Set<String> set){
        List<History>h=new ArrayList<>(set.size());
        for(String s:set)h.add(new History(s,true));
        Collections.sort(h, (o2, o1) -> {
            int o=o1.date.compareTo(o2.date);
            if(o==0)o=o1.value.compareTo(o2.value);
            return o;
        });

        return h;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        History history = (History) o;
        return value.equals(history.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    public static Set<String>listToSet(List<History>list){
        HashSet<String>s=new HashSet<>(list.size());
        for (History h:list)s.add(h.date.getTime()+"|"+h.value);
        return s;
    }
}
