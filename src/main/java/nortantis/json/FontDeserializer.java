package nortantis.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.awt.*;
import java.io.IOException;

public class FontDeserializer extends StdDeserializer<Font> {

    public FontDeserializer() {
        this(Font.class);
    }

    public FontDeserializer(Class<Font> vc) {
        super(vc);
    }

    @Override
    public Font deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        return Font.decode(node.asText());
    }
}