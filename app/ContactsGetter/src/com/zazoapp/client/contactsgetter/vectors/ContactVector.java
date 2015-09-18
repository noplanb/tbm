package com.zazoapp.client.contactsgetter.vectors;

import com.google.gson.internal.LinkedTreeMap;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Created by skamenkovych@codeminders.com on 9/17/2015.
 */
public abstract class ContactVector {
    protected String name;
    protected String value;
    protected LinkedTreeMap<String, Object> additions = new LinkedTreeMap<>();

    ContactVector(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public void addParam(String paramKey, Object paramValue) {
        additions.put(paramKey, paramValue);
    }

    public abstract String normaliseValue();

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ContactVector) {
            ContactVector cv = (ContactVector) o;
            return this == cv || name != null && name.equals(cv.name) && value != null && value.equals(cv.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31). // two randomly chosen prime numbers
                // if deriving: appendSuper(super.hashCode()).
                append(name).
                append(value).
                toHashCode();
    }
}
