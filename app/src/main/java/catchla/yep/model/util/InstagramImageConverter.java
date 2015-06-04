package catchla.yep.model.util;

import com.bluelinelabs.logansquare.LoganSquare;
import com.bluelinelabs.logansquare.typeconverters.TypeConverter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

import catchla.yep.model.InstagramImage;
import io.realm.RealmList;

/**
 * Created by mariotaku on 15/6/2.
 */
public class InstagramImageConverter implements TypeConverter<RealmList<InstagramImage>> {
    @Override
    public RealmList<InstagramImage> parse(final JsonParser jsonParser) throws IOException {
        final RealmList<InstagramImage> providers = new RealmList<>();
        if (jsonParser.getCurrentToken() == null) {
            jsonParser.nextToken();
        }
        if (jsonParser.getCurrentToken() != JsonToken.START_OBJECT) {
            jsonParser.skipChildren();
            return null;
        }
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            final String resolution = jsonParser.getCurrentName();
            jsonParser.nextToken();
            final InstagramImage image = LoganSquare.mapperFor(InstagramImage.class).parse(jsonParser);
            image.setResolution(resolution);
            providers.add(image);
            jsonParser.skipChildren();
        }
        return providers;
    }

    @Override
    public void serialize(final RealmList<InstagramImage> object, String fieldName, boolean writeFieldNameForObject,
                          JsonGenerator jsonGenerator) throws IOException {
        if (object != null) {
            if (writeFieldNameForObject) {
                jsonGenerator.writeFieldName(fieldName);
            }
            jsonGenerator.writeStartObject();
            for (InstagramImage provider : object) {
                jsonGenerator.writeFieldName(provider.getResolution());
                jsonGenerator.writeStartObject();
                jsonGenerator.writeFieldName("url");
                jsonGenerator.writeString(provider.getUrl());
                jsonGenerator.writeFieldName("width");
                jsonGenerator.writeNumber(provider.getWidth());
                jsonGenerator.writeFieldName("height");
                jsonGenerator.writeNumber(provider.getHeight());
                jsonGenerator.writeEndObject();
            }
            jsonGenerator.writeEndObject();
        }
    }
}
