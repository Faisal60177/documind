package com.documind.rag;

import java.util.List;

/**
 * The model's reply after parsing. {@code text} has its citation markers renumbered in order of
 * first appearance. {@code citedContextNumbers} lists the original context numbers in that order,
 * so the Nth entry is the source shown as [N].
 */
record ParsedAnswer(boolean found, String text, List<Integer> citedContextNumbers) {}
