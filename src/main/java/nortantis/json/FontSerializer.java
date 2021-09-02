package nortantis.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.awt.*;
import java.io.IOException;

import static java.lang.String.format;

public class FontSerializer extends StdSerializer<Font> {

    public FontSerializer(Class<Font> t) {
        super(t);
    }

    @Override
    public void serialize(Font font, JsonGenerator generator, SerializerProvider provider) throws IOException {
        generator.writeString(format("%s %d %d", font.getFontName(), font.getStyle(), font.getSize()));
    }
}
