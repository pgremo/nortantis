package nortantis.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.awt.*;
import java.io.IOException;

import static java.lang.String.format;

public class ColorSerializer extends StdSerializer<Color> {

    public ColorSerializer(Class<Color> t) {
        super(t);
    }

    @Override
    public void serialize(Color color, JsonGenerator generator, SerializerProvider provider) throws IOException {
        generator.writeString(format("%d,%d,%d,%d", color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()));
    }
}
