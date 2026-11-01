package com.scytalys.trackpicture.message;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum Identity {
    UNKNOWN,
    FRIEND,
    NEUTRAL,
    HOSTILE;

    @JsonCreator
    public static Identity from(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
