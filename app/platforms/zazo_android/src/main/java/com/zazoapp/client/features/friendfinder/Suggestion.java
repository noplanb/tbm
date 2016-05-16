package com.zazoapp.client.features.friendfinder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by skamenkovych@codeminders.com on 4/28/2016.
 */
public class Suggestion {
    private String name;
    private List<String> phones;

    public Suggestion(String name) {
        this.name = name;
        this.phones = new ArrayList<>();
    }

    public Suggestion(String name, List<String> phones) {
        this.name = name;
        this.phones = (phones != null) ? phones : new ArrayList<String>();
    }

    public List<String> getPhones() {
        return phones;
    }

    public String getPhone(int index) {
        return (phones.size() > index && index >= 0) ? phones.get(index) : null;
    }

    public String getName() {
        return name;
    }

    public boolean hasMultiplePhones() {
        return phones.size() > 1;
    }
}
