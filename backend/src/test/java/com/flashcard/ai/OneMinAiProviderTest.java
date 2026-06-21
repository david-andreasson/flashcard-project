package com.flashcard.ai;

import com.flashcard.common.UpstreamAiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OneMinAiProviderTest {

    private static final OneMinProperties PROPS =
            new OneMinProperties("https://api.1min.ai", "test-key", "claude-haiku-4-5-20251001");
    private static final String URL = "https://api.1min.ai/api/chat-with-ai";

    @Test
    void mapsResultObjectArray_estimatesTokens_andSendsApiKeyHeader() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OneMinAiProvider provider = new OneMinAiProvider(PROPS, builder);

        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("API-KEY", "test-key"))
                .andRespond(withSuccess(
                        "{\"aiRecord\":{\"aiRecordDetail\":{\"resultObject\":[\"hello world\"]}}}",
                        MediaType.APPLICATION_JSON));

        AiResponse resp = provider.complete(new AiRequest("sys", "user", 100, "card-generation"));

        assertThat(resp.content()).isEqualTo("hello world");
        assertThat(resp.inputTokens()).isPositive();
        assertThat(resp.outputTokens()).isPositive();
        assertThat(resp.modelId()).isEqualTo("claude-haiku-4-5-20251001");
        server.verify();
    }

    @Test
    void mapsResultObjectAsPlainText() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OneMinAiProvider provider = new OneMinAiProvider(PROPS, builder);

        server.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"aiRecord\":{\"aiRecordDetail\":{\"resultObject\":\"plain\"}}}",
                MediaType.APPLICATION_JSON));

        assertThat(provider.complete(new AiRequest(null, "user", 100, "echo")).content())
                .isEqualTo("plain");
    }

    @Test
    void upstreamError_throwsUpstreamAiException() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OneMinAiProvider provider = new OneMinAiProvider(PROPS, builder);

        server.expect(requestTo(URL)).andRespond(withServerError());

        assertThatThrownBy(() -> provider.complete(new AiRequest("s", "u", 100, "echo")))
                .isInstanceOf(UpstreamAiException.class);
    }

    @Test
    void missingResultObject_throwsUpstreamAiException() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OneMinAiProvider provider = new OneMinAiProvider(PROPS, builder);

        server.expect(requestTo(URL)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.complete(new AiRequest("s", "u", 100, "echo")))
                .isInstanceOf(UpstreamAiException.class);
    }
}
