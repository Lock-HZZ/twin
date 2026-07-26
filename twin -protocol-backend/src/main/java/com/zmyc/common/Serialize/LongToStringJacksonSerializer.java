package com.zmyc.common.Serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class LongToStringJacksonSerializer extends JsonSerializer<Long> {

    public static final long MAX_LONG_TO_STRING = (long) Math.pow(10, 15);

    @Override
    public void serialize(Long aLong,
                          JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider)
            throws IOException {
        if (aLong != null) {
            if (aLong > MAX_LONG_TO_STRING) {
                jsonGenerator.writeString(aLong.toString());
            } else {
                jsonGenerator.writeNumber(aLong);
            }
        }
    }

}
