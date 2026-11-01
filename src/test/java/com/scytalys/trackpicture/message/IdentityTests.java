package com.scytalys.trackpicture.message;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.exc.ValueInstantiationException;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentityTests {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    @ParameterizedTest
    @ValueSource(strings = {"hostile", "HOSTILE", "HoStIlE"})
    void deserializesIdentityCaseInsensitively(String value) {
        Identity identity = JSON_MAPPER.readValue("\"" + value + "\"", Identity.class);

        assertThat(identity).isEqualTo(Identity.HOSTILE);
    }

    @Test
    void rejectsUnknownIdentity() {
        assertThatThrownBy(() -> JSON_MAPPER.readValue("\"pilot\"", Identity.class))
                .isInstanceOf(ValueInstantiationException.class);
    }

    @Test
    void serializesIdentityUsingUppercaseEnumName() {
        String json = JSON_MAPPER.writeValueAsString(Identity.HOSTILE);

        assertThat(json).isEqualTo("\"HOSTILE\"");
    }
}
