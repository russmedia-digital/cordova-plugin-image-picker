package com.imagepicker.plus;

import org.json.JSONException;
import org.json.JSONObject;

public class Options {
    int selectionLimit = 15;
    int quality = 100;
    int maxWidth = 0;
    int maxHeight = 0;
    int outputType = 0; // Default uri
    String maxCountMessage; // Default "You can select a maximum of %d pictures"

    boolean isSingleSelect() {
        return selectionLimit == 1;
    }

    boolean isBase64Output() {
        return outputType != 0;
    }

    String getMaxCountMessage() {
        if (maxCountMessage != null) {
            return maxCountMessage;
        }
        return String.format("You can select a maximum of %d pictures", selectionLimit);
    }

    Options(JSONObject params) throws JSONException {
        if (params.has("maximumImagesCount")) {
            selectionLimit = params.getInt("maximumImagesCount");
        }
        if (params.has("width")) {
            maxWidth = params.getInt("width");
        }
        if (params.has("height")) {
            maxHeight = params.getInt("height");
        }
        if (params.has("quality")) {
            quality = params.getInt("quality");
        }
        if (params.has("outputType")) {
            outputType = params.getInt("outputType");
        }
        if (params.has("maxCountMessage")) {
            maxCountMessage = params.getString("maxCountMessage");
        }
    }
}
