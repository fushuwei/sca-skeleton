package io.github.fushuwei.scaskeleton.security.oauth2;

import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2DeviceCode;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.OAuth2UserCode;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 将 {@link OAuth2Authorization} 映射为与 SAS JDBC 插入语句相同顺序的 {@link SqlParameterValue} 列表。
 * <p>
 * SAS 7 的 {@code JdbcOAuth2AuthorizationService.JsonMapperOAuth2AuthorizationParametersMapper} 依赖
 * {@code JdbcOAuth2AuthorizationService} 构造时初始化的静态 {@code columnMetadataMap}；纯 Redis 存储场景下
 * 未创建 JDBC 服务会导致 NPE。本类复刻官方 {@code AbstractOAuth2AuthorizationParametersMapper} 逻辑，
 * 对 BLOB 列使用固定 {@link Types#BLOB}，不依赖 JDBC 元数据。
 *
 * @author Fu Wei
 */
final class RedisOAuth2AuthorizationParametersMapper implements Function<OAuth2Authorization, List<SqlParameterValue>> {

    private final JsonMapper jsonMapper;

    RedisOAuth2AuthorizationParametersMapper(JsonMapper jsonMapper) {
        Assert.notNull(jsonMapper, "jsonMapper cannot be null");
        this.jsonMapper = jsonMapper;
    }

    @Override
    public List<SqlParameterValue> apply(OAuth2Authorization authorization) {
        List<SqlParameterValue> parameters = new ArrayList<>();
        parameters.add(new SqlParameterValue(Types.VARCHAR, authorization.getId()));
        parameters.add(new SqlParameterValue(Types.VARCHAR, authorization.getRegisteredClientId()));
        parameters.add(new SqlParameterValue(Types.VARCHAR, authorization.getPrincipalName()));
        parameters.add(new SqlParameterValue(Types.VARCHAR, authorization.getAuthorizationGrantType().getValue()));

        String authorizedScopes = null;
        if (!CollectionUtils.isEmpty(authorization.getAuthorizedScopes())) {
            authorizedScopes = StringUtils.collectionToDelimitedString(authorization.getAuthorizedScopes(), ",");
        }
        parameters.add(new SqlParameterValue(Types.VARCHAR, authorizedScopes));

        String attributes = writeMap(authorization.getAttributes());
        parameters.add(blobParameter(attributes));

        String state = null;
        String authorizationState = authorization.getAttribute(OAuth2ParameterNames.STATE);
        if (StringUtils.hasText(authorizationState)) {
            state = authorizationState;
        }
        parameters.add(new SqlParameterValue(Types.VARCHAR, state));

        OAuth2Authorization.Token<OAuth2AuthorizationCode> authorizationCode =
                authorization.getToken(OAuth2AuthorizationCode.class);
        parameters.addAll(toSqlParameterList(authorizationCode));

        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getToken(OAuth2AccessToken.class);
        parameters.addAll(toSqlParameterList(accessToken));
        String accessTokenType = null;
        String accessTokenScopes = null;
        if (accessToken != null) {
            accessTokenType = accessToken.getToken().getTokenType().getValue();
            if (!CollectionUtils.isEmpty(accessToken.getToken().getScopes())) {
                accessTokenScopes = StringUtils.collectionToDelimitedString(accessToken.getToken().getScopes(), ",");
            }
        }
        parameters.add(new SqlParameterValue(Types.VARCHAR, accessTokenType));
        parameters.add(new SqlParameterValue(Types.VARCHAR, accessTokenScopes));

        OAuth2Authorization.Token<OidcIdToken> oidcIdToken = authorization.getToken(OidcIdToken.class);
        parameters.addAll(toSqlParameterList(oidcIdToken));

        OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = authorization.getRefreshToken();
        parameters.addAll(toSqlParameterList(refreshToken));

        OAuth2Authorization.Token<OAuth2UserCode> userCode = authorization.getToken(OAuth2UserCode.class);
        parameters.addAll(toSqlParameterList(userCode));

        OAuth2Authorization.Token<OAuth2DeviceCode> deviceCode = authorization.getToken(OAuth2DeviceCode.class);
        parameters.addAll(toSqlParameterList(deviceCode));

        return parameters;
    }

    private <T extends OAuth2Token> List<SqlParameterValue> toSqlParameterList(OAuth2Authorization.Token<T> token) {
        List<SqlParameterValue> parameters = new ArrayList<>();
        String tokenValue = null;
        Timestamp tokenIssuedAt = null;
        Timestamp tokenExpiresAt = null;
        String metadata = null;
        if (token != null) {
            tokenValue = token.getToken().getTokenValue();
            if (token.getToken().getIssuedAt() != null) {
                tokenIssuedAt = Timestamp.from(token.getToken().getIssuedAt());
            }
            if (token.getToken().getExpiresAt() != null) {
                tokenExpiresAt = Timestamp.from(token.getToken().getExpiresAt());
            }
            metadata = writeMap(token.getMetadata());
        }

        parameters.add(blobParameter(tokenValue));
        parameters.add(new SqlParameterValue(Types.TIMESTAMP, tokenIssuedAt));
        parameters.add(new SqlParameterValue(Types.TIMESTAMP, tokenExpiresAt));
        parameters.add(blobParameter(metadata));
        return parameters;
    }

    private String writeMap(Map<String, Object> data) {
        try {
            return this.jsonMapper.writeValueAsString(data);
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

    private static SqlParameterValue blobParameter(String value) {
        if (StringUtils.hasText(value)) {
            return new SqlParameterValue(Types.BLOB, value.getBytes(StandardCharsets.UTF_8));
        }
        return new SqlParameterValue(Types.BLOB, null);
    }
}
