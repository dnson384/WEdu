package com.wedu.exam_creation.moderator.usecase;

/** Question media keys use the media/ namespace; all other values are AI descriptions. */
public final class QuestionImageReference {
    private QuestionImageReference() {
    }

    public static boolean isStoredKey(String value) {
        return value != null && value.startsWith("media/") && value.length() > 6
                && !value.contains("://") && !value.contains("\\")
                && java.util.Arrays.stream(value.split("/", -1))
                .noneMatch(segment -> segment.isEmpty() || segment.equals(".") || segment.equals(".."));
    }
}
