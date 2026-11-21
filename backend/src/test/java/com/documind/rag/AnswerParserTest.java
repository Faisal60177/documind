package com.documind.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class AnswerParserTest {

    @Test
    void recognisesTheNotFoundMarker() {
        ParsedAnswer parsed = AnswerParser.parse("[[NOT_FOUND]] The documents do not contain this.", 5);

        assertThat(parsed.found()).isFalse();
        assertThat(parsed.text()).isEqualTo("The documents do not contain this.");
        assertThat(parsed.citedContextNumbers()).isEmpty();
    }

    @Test
    void renumbersCitationsInOrderOfFirstAppearance() {
        ParsedAnswer parsed = AnswerParser.parse("Alpha [3]. Beta [1][3]. Gamma [2].", 5);

        assertThat(parsed.found()).isTrue();
        assertThat(parsed.text()).isEqualTo("Alpha [1]. Beta [2][1]. Gamma [3].");
        assertThat(parsed.citedContextNumbers()).containsExactly(3, 1, 2);
    }

    @Test
    void expandsGroupedCitations() {
        ParsedAnswer parsed = AnswerParser.parse("Fact [1, 2].", 3);

        assertThat(parsed.text()).isEqualTo("Fact [1][2].");
        assertThat(parsed.citedContextNumbers()).containsExactly(1, 2);
    }

    @Test
    void dropsCitationsToPassagesThatDoNotExist() {
        ParsedAnswer parsed = AnswerParser.parse("Fact [9].", 3);

        assertThat(parsed.found()).isTrue();
        assertThat(parsed.citedContextNumbers()).isEqualTo(List.of());
    }

    @Test
    void removesAMarkerThatAppearsInTheMiddleOfAnAnswer() {
        ParsedAnswer parsed = AnswerParser.parse("Some answer [1]. [[NOT_FOUND]]", 2);

        assertThat(parsed.found()).isTrue();
        assertThat(parsed.text()).isEqualTo("Some answer [1].");
    }
}
