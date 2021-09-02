package nortantis.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.awt.*;
import java.io.IOException;

public class ColorDeserializer extends StdDeserializer<Color> {

    public ColorDeserializer() {
        this(Color.class);
    }

    public ColorDeserializer(Class<Color> vc) {
        super(vc);
    }

    @Override
    public Color deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        var str = node.asText();
        if (str == null) return null;
        var parts = str.split(",");
        switch (parts.length) {
            case 3:
                return new Color(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            case 4:
                return new Color(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
            default:
                context.reportInputMismatch(Color.class, "invalid color format [%s]", str);
                throw new IllegalArgumentException("Unable to parse color from string: " + str);
        }
    }
}