package com.dsi.support.agenticrouter.service.knowledge;

import lombok.extern.slf4j.Slf4j;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
public class OpenNlpTokenizer {

    private final Tokenizer tokenizer;
    private final SentenceDetectorME sentenceDetector;

    public OpenNlpTokenizer() {
        this.tokenizer = SimpleTokenizer.INSTANCE;
        this.sentenceDetector = loadSentenceDetector();
    }

    public List<String> tokenize(
        String text
    ) {
        String normalized = StringUtils.defaultString(text);

        if (StringUtils.isBlank(normalized)) {
            return List.of();
        }

        String[] tokens = tokenizer.tokenize(
            normalized
        );

        return Arrays.asList(tokens);
    }

    public int tokenCount(
        String text
    ) {
        return tokenize(text).size();
    }

    public List<String> detectSentences(
        String text
    ) {
        String normalized = StringUtils.defaultString(text);

        if (StringUtils.isBlank(normalized)) {
            return List.of();
        }

        String[] sentences = sentenceDetector.sentDetect(
            normalized
        );

        return Arrays.asList(sentences);
    }

    private SentenceDetectorME loadSentenceDetector() {
        try (InputStream modelIn = getClass().getResourceAsStream(
            "/opennlp/en-sent.bin"
        )) {
            if (Objects.isNull(modelIn)) {
                throw new IllegalStateException(
                    "Missing OpenNLP sentence model resource: /opennlp/en-sent.bin"
                );
            }

            SentenceModel model = new SentenceModel(
                modelIn
            );

            return new SentenceDetectorME(
                model
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Failed to load OpenNLP sentence model resource: /opennlp/en-sent.bin",
                exception
            );
        }
    }
}
