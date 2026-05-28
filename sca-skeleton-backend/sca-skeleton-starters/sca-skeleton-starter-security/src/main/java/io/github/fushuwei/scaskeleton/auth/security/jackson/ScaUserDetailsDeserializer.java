package io.github.fushuwei.scaskeleton.auth.security.jackson;

import io.github.fushuwei.scaskeleton.auth.security.ScaUserDetails;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.node.MissingNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 从 OAuth2Authorization.attributes 中还原 {@link ScaUserDetails}。
 */
class ScaUserDetailsDeserializer extends ValueDeserializer<ScaUserDetails> {

    @Override
    public ScaUserDetails deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
        JsonNode node = context.readTree(parser);
        return new ScaUserDetails(
                readText(node, "userId"),
                readText(node, "tenantId"),
                readText(node, "username"),
                readTextOrNull(node, "password"),
                readText(node, "nickname"),
                readText(node, "userType"),
                readStringList(node, "permissions"),
                readBoolean(node, "enabled", true),
                readBoolean(node, "accountNonLocked", true),
                readBoolean(node, "accountNonExpired", true),
                readBoolean(node, "credentialsNonExpired", true));
    }

    private static String readText(JsonNode node, String field) {
        JsonNode value = readField(node, field);
        return value.isMissingNode() ? "" : value.asString();
    }

    private static String readTextOrNull(JsonNode node, String field) {
        JsonNode value = readField(node, field);
        return value.isMissingNode() ? null : value.asString(null);
    }

    private static boolean readBoolean(JsonNode node, String field, boolean defaultValue) {
        JsonNode value = readField(node, field);
        return value.isMissingNode() ? defaultValue : value.asBoolean(defaultValue);
    }

    private static List<String> readStringList(JsonNode node, String field) {
        JsonNode value = readField(node, field);
        if (value.isMissingNode()) {
            return List.of();
        }
        JsonNode arrayNode = unwrapTypedCollection(value);
        if (!arrayNode.isArray()) {
            return List.of();
        }
        List<String> items = new ArrayList<>();
        arrayNode.forEach(item -> {
            if (item != null && !item.isNull()) {
                items.add(item.asString());
            }
        });
        return items;
    }

    /** 处理 Security Jackson 默认类型包装：{@code ["java.util.ArrayList", [...]]} */
    private static JsonNode unwrapTypedCollection(JsonNode value) {
        if (value.isArray() && value.size() == 2 && value.get(0).isString()) {
            return value.get(1);
        }
        return value;
    }

    private static JsonNode readField(JsonNode node, String field) {
        return node.has(field) ? node.get(field) : MissingNode.getInstance();
    }
}
