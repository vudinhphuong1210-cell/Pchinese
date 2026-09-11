package net.pchinese.dictionary.application;

import net.pchinese.dictionary.domain.QueryKind;
import net.pchinese.dictionary.persistence.DictionaryEntryEntity;
import net.pchinese.dictionary.persistence.DictionarySearchKeyEntity;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DictionarySearchKeyProjector {

    private static final Pattern VIETNAMESE_MEANING_PATTERN = Pattern.compile("\"meaning_vi\"\\s*:\\s*\"([^\"]+)\"");

    public String normalizePinyin(String rawPinyin) {
        if (rawPinyin == null) {
            return "";
        }
        // Decompose accents/diacritics and strip tone marks
        String nfkd = Normalizer.normalize(rawPinyin.toLowerCase(), Normalizer.Form.NFD);
        String withoutTones = nfkd.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        // Normalize ü / v
        withoutTones = withoutTones.replace("ü", "u").replace("v", "u");
        // Remove spaces, punctuation, quotes
        return withoutTones.replaceAll("[^a-z0-9]", "");
    }

    public String normalizeHanzi(String rawHanzi) {
        if (rawHanzi == null) {
            return "";
        }
        return rawHanzi.trim();
    }

    public String normalizeVietnamese(String text) {
        if (text == null) {
            return "";
        }
        String nfkd = Normalizer.normalize(text.toLowerCase(), Normalizer.Form.NFD);
        String withoutDiacriticals = nfkd.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return withoutDiacriticals.replace("đ", "d").trim();
    }

    public List<DictionarySearchKeyEntity> generateSearchKeys(DictionaryEntryEntity entry) {
        List<DictionarySearchKeyEntity> keys = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // 1. Simplified Hanzi
        if (entry.getSimplifiedHanzi() != null && !entry.getSimplifiedHanzi().isBlank()) {
            addHanziKeys(entry, QueryKind.SIMPLIFIED_HANZI, entry.getSimplifiedHanzi().trim(), keys, seen);
        }

        // 2. Traditional Hanzi
        if (entry.getTraditionalHanzi() != null && !entry.getTraditionalHanzi().isBlank()) {
            addHanziKeys(entry, QueryKind.TRADITIONAL_HANZI, entry.getTraditionalHanzi().trim(), keys, seen);
        }

        // 3. Pinyin
        if (entry.getPrimaryPinyin() != null && !entry.getPrimaryPinyin().isBlank()) {
            String normPinyin = normalizePinyin(entry.getPrimaryPinyin());
            if (!normPinyin.isEmpty()) {
                addPinyinKeys(entry, QueryKind.PINYIN, normPinyin, keys, seen);
            }
        }

        // 4. Vietnamese Meaning Keywords
        if (entry.getSenses() != null && !entry.getSenses().isBlank()) {
            Matcher matcher = VIETNAMESE_MEANING_PATTERN.matcher(entry.getSenses());
            while (matcher.find()) {
                String meaning = matcher.group(1);
                String[] words = meaning.split("[,;\\s]+");
                for (String word : words) {
                    String normWord = normalizeVietnamese(word);
                    if (normWord.length() >= 1) {
                        addKeyIfAbsent(entry, QueryKind.VIETNAMESE_KEYWORD, normWord, (short) 1, keys, seen);
                    }
                }
            }
        }

        return keys;
    }

    private void addHanziKeys(DictionaryEntryEntity entry, QueryKind kind, String hanzi, List<DictionarySearchKeyEntity> keys, Set<String> seen) {
        int length = hanzi.codePointCount(0, hanzi.length());
        for (int i = 0; i < length; i++) {
            for (int j = i + 1; j <= length; j++) {
                String sub = substringCodePoints(hanzi, i, j);
                short rank;
                if (i == 0 && j == length) {
                    rank = 1; // Whole-field match
                } else if (i == 0) {
                    rank = 2; // Prefix match
                } else {
                    rank = 3; // Interior substring match
                }
                addKeyIfAbsent(entry, kind, sub, rank, keys, seen);
            }
        }
    }

    private void addPinyinKeys(DictionaryEntryEntity entry, QueryKind kind, String normPinyin, List<DictionarySearchKeyEntity> keys, Set<String> seen) {
        int length = normPinyin.length();
        for (int i = 0; i < length; i++) {
            for (int j = i + 1; j <= length; j++) {
                String sub = normPinyin.substring(i, j);
                short rank;
                if (i == 0 && j == length) {
                    rank = 1; // Whole-field
                } else if (i == 0) {
                    rank = 2; // Prefix
                } else {
                    rank = 3; // Interior substring
                }
                addKeyIfAbsent(entry, kind, sub, rank, keys, seen);
            }
        }
    }

    private void addKeyIfAbsent(DictionaryEntryEntity entry, QueryKind kind, String key, short rank, List<DictionarySearchKeyEntity> keys, Set<String> seen) {
        String dedupeKey = kind.name() + ":" + key;
        if (seen.add(dedupeKey)) {
            DictionarySearchKeyEntity keyEntity = new DictionarySearchKeyEntity();
            keyEntity.setDictionaryEntryId(entry.getDictionaryEntryId());
            keyEntity.setQueryKind(kind);
            keyEntity.setNormalizedKey(key);
            keyEntity.setMatchRank(rank);
            keys.add(keyEntity);
        }
    }

    private String substringCodePoints(String str, int startCodePoint, int endCodePoint) {
        int start = str.offsetByCodePoints(0, startCodePoint);
        int end = str.offsetByCodePoints(0, endCodePoint);
        return str.substring(start, end);
    }
}
