from __future__ import annotations

from typing import Callable


PhraseReadings = dict[str, tuple[str, ...]]


# Proper names and fixed corpus phrases. This table is intentionally plain data:
# add new phrases here first, then extend validator tests when a regression needs
# to become a guardrail.
PROPER_NAME_READINGS: PhraseReadings = {
    "长坂坡": ("cháng", "bǎn", "pō"),
    "华容道": ("huá", "róng", "dào"),
    "诸葛亮": ("zhū", "gě", "liàng"),
    "刘备": ("liú", "bèi"),
    "关羽": ("guān", "yǔ"),
    "张飞": ("zhāng", "fēi"),
    "吕布": ("lǚ", "bù"),
    "鲁肃": ("lǔ", "sù"),
    "周瑜": ("zhōu", "yú"),
    "孟获": ("mèng", "huò"),
    "司马懿": ("sī", "mǎ", "yì"),
    "曹操": ("cáo", "cāo"),
    "赵云": ("zhào", "yún"),
    "孙权": ("sūn", "quán"),
    "黄盖": ("huáng", "gài"),
    "涿县": ("zhuō", "xiàn"),
    "虎牢关": ("hǔ", "láo", "guān"),
    "隆中": ("lóng", "zhōng"),
    "赤壁": ("chì", "bì"),
    "荆州": ("jīng", "zhōu"),
    "襄阳": ("xiāng", "yáng"),
    "檀溪": ("tán", "xī"),
    "的卢": ("dì", "lú"),
    "陆口": ("lù", "kǒu"),
    "刘禅": ("liú", "shàn"),
    "成都": ("chéng", "dū"),
    "马谡": ("mǎ", "sù"),
    "王平": ("wáng", "píng"),
    "街亭": ("jiē", "tíng"),
}


CORPUS_PHRASE_READINGS: PhraseReadings = {
    "长叹": ("cháng", "tàn"),
    "路又长": ("lù", "yòu", "cháng"),
    "结为兄弟": ("jié", "wéi", "xiōng", "dì"),
    "为百姓着想": ("wèi", "bǎi", "xìng", "zhuó", "xiǎng"),
    "请教": ("qǐng", "jiào"),
    "交还": ("jiāo", "huán"),
    "扎满": ("zā", "mǎn"),
    "扎满草把": ("zā", "mǎn", "cǎo", "bǎ"),
    "这个": ("zhè", "ge"),
    "如数": ("rú", "shù"),
    "分量": ("fèn", "liàng"),
    "重信用": ("zhòng", "xìn", "yòng"),
    "地方": ("dì", "fāng"),
    "商量": ("shāng", "liáng"),
    "弹琴": ("tán", "qín"),
    "为了": ("wèi", "le"),
    "因为": ("yīn", "wèi"),
    "听了": ("tīng", "le"),
    "到了": ("dào", "le"),
    "来了": ("lái", "le"),
    "看了看": ("kàn", "le", "kàn"),
    "更难": ("gèng", "nán"),
    "仿佛": ("fǎng", "fú"),
    "会儿": ("huì", "er"),
    "谁": ("shuí",),
    "船只": ("chuán", "zhī"),
    "日子": ("rì", "zi"),
    "旗子": ("qí", "zi"),
    "梅子": ("méi", "zi"),
    "架子": ("jià", "zi"),
    "孩子": ("hái", "zi"),
    "车子": ("chē", "zi"),
    "一下子": ("yī", "xià", "zi"),
    "盼着": ("pàn", "zhe"),
    "冒着": ("mào", "zhe"),
    "笑着": ("xiào", "zhe"),
    "着急": ("zháo", "jí"),
    "急着": ("jí", "zhe"),
    "坐着": ("zuò", "zhe"),
    "看着": ("kàn", "zhe"),
    "带着": ("dài", "zhe"),
    "试着": ("shì", "zhe"),
    "装着": ("zhuāng", "zhe"),
    "着想": ("zhuó", "xiǎng"),
    "沉着": ("chén", "zhuó"),
    "一向": ("yí", "xiàng"),
    "重新": ("chóng", "xīn"),
    "开得": ("kāi", "de"),
    "吹得": ("chuī", "de"),
    "走得": ("zǒu", "de"),
    "排得": ("pái", "de"),
    "说得": ("shuō", "de"),
    "称得上": ("chēng", "de", "shàng"),
    "讲得": ("jiǎng", "de"),
    "不由得": ("bù", "yóu", "de"),
    "觉得": ("jué", "de"),
    "记得": ("jì", "de"),
    "懂得": ("dǒng", "de"),
    "显得": ("xiǎn", "de"),
    "恭恭敬敬地": ("gōng", "gōng", "jìng", "jìng", "de"),
    "有礼貌地": ("yǒu", "lǐ", "mào", "de"),
    "一下一下地": ("yī", "xià", "yī", "xià", "de"),
    "慢慢地": ("màn", "màn", "de"),
    "谦虚地": ("qiān", "xū", "de"),
    "扫地": ("sǎo", "dì"),
    "得到": ("dé", "dào"),
    "同行": ("tóng", "xíng"),
    "前行": ("qián", "xíng"),
    "行动": ("xíng", "dòng"),
    "山路又长": ("shān", "lù", "yòu", "cháng"),
    "又长又弯": ("yòu", "cháng", "yòu", "wān"),
    "载具": ("zài", "jù"),
    "转动的木轮": ("zhuàn", "dòng", "de", "mù", "lún"),
    "转了方向": ("zhuǎn", "le", "fāng", "xiàng"),
    "转弯": ("zhuǎn", "wān"),
    "转向": ("zhuǎn", "xiàng"),
    "木牛流马": ("mù", "niú", "liú", "mǎ"),
    "省力": ("shěng", "lì"),
    "兄长": ("xiōng", "zhǎng"),
    "出师表": ("chū", "shī", "biǎo"),
    "表章": ("biǎo", "zhāng"),
    "赴会": ("fù", "huì"),
    "单刀赴会": ("dān", "dāo", "fù", "huì"),
    "长刀": ("cháng", "dāo"),
    "会面": ("huì", "miàn"),
    "好好说话": ("hǎo", "hǎo", "shuō", "huà"),
    "只带": ("zhǐ", "dài"),
    "只顾": ("zhǐ", "gù"),
    "听取": ("tīng", "qǔ"),
    "扎营": ("zhā", "yíng"),
    "扎在": ("zhā", "zài"),
    "过了": ("guò", "le"),
    "乱了": ("luàn", "le"),
    "写成": ("xiě", "chéng"),
    "读着": ("dú", "zhe"),
    "听着": ("tīng", "zhe"),
    "想着": ("xiǎng", "zhe"),
    "沿着": ("yán", "zhe"),
    "指着": ("zhǐ", "zhe"),
    "佩着": ("pèi", "zhe"),
    "摸着": ("mō", "zhe"),
    "做得": ("zuò", "de"),
}


PINYIN_PHRASE_OVERRIDES: PhraseReadings = {
    **PROPER_NAME_READINGS,
    **CORPUS_PHRASE_READINGS,
}


# Words where 得 keeps a full tone. Keep this narrow; many common 得 compounds
# in this children corpus are neutral-tone lexical words (觉得/记得/懂得).
DE_FULL_TONE_WORDS = {
    "得到",
    "得意",
    "得力",
    "得知",
    "得胜",
    "得分",
    "得当",
    "获得",
    "取得",
    "赢得",
    "博得",
    "求得",
    "所得",
    "得失",
}

DE_NEUTRAL_WORDS = {
    "觉得",
    "记得",
    "懂得",
    "显得",
    "晓得",
    "舍得",
    "免得",
    "不由得",
    "怪不得",
    "使得",
    "省得",
}

DE_MUST_WORDS = {
    "非得",
    "总得",
    "得要",
    "得先",
    "得去",
}

DI_NOUN_WORDS = {
    "地方",
    "地上",
    "地下",
    "地面",
    "地里",
    "地点",
    "土地",
    "天地",
    "大地",
    "当地",
    "各地",
    "本地",
    "此地",
    "那地",
    "这地",
    "地区",
    "地图",
    "地球",
    "地位",
    "地步",
    "田地",
    "境地",
    "余地",
    "阵地",
    "地名",
    "地址",
    "扫地",
    "种地",
    "落地",
    "倒地",
    "遍地",
    "就地",
    "平地",
    "空地",
    "外地",
    "产地",
    "基地",
    "陆地",
    "草地",
    "园地",
}


POLYPHONE_GUARDRAIL_PHRASES: PhraseReadings = {
    key: PINYIN_PHRASE_OVERRIDES[key]
    for key in (
        "长坂坡",
        "长叹",
        "华容道",
        "诸葛亮",
        "黄盖",
        "路又长",
        "如数",
        "分量",
        "重信用",
        "为百姓着想",
        "请教",
        "盼着",
        "着急",
        "着想",
        "沉着",
        "觉得",
        "记得",
        "懂得",
        "显得",
        "称得上",
        "交还",
        "重新",
        "同行",
        "行动",
        "又长又弯",
        "载具",
        "转动的木轮",
        "转了方向",
        "转弯",
        "转向",
        "冒着",
        "笑着",
        "檀溪",
        "的卢",
        "陆口",
        "刘禅",
        "出师表",
        "马谡",
        "王平",
        "街亭",
        "赴会",
        "单刀赴会",
        "长刀",
        "扎营",
    )
}


def _find_phrase_starts(text: str, phrase: str) -> list[int]:
    starts: list[int] = []
    start = text.find(phrase)
    while start >= 0:
        starts.append(start)
        start = text.find(phrase, start + 1)
    return starts


def apply_phrase_readings(
    text: str,
    tokens: list[str],
    phrase_readings: PhraseReadings = PINYIN_PHRASE_OVERRIDES,
) -> list[bool]:
    locked = [False] * len(tokens)
    for phrase, readings in sorted(phrase_readings.items(), key=lambda item: len(item[0]), reverse=True):
        if len(phrase) != len(readings):
            raise ValueError(f"phrase override {phrase!r} has {len(readings)} readings for {len(phrase)} chars")
        for start in _find_phrase_starts(text, phrase):
            for offset, reading in enumerate(readings):
                index = start + offset
                tokens[index] = reading
                locked[index] = True
    return locked


def _context_words(text: str, index: int) -> tuple[str, str]:
    prev_char = text[index - 1] if index > 0 else ""
    next_char = text[index + 1] if index + 1 < len(text) else ""
    char = text[index]
    return prev_char + char, char + next_char


def apply_contextual_readings(
    text: str,
    tokens: list[str],
    locked: list[bool],
    *,
    is_hanzi_char: Callable[[str], bool],
) -> None:
    for index, char in enumerate(text):
        prev_char = text[index - 1] if index > 0 else ""
        next_char = text[index + 1] if index + 1 < len(text) else ""
        prev_is_hanzi = is_hanzi_char(prev_char)
        next_is_hanzi = is_hanzi_char(next_char)
        word_prev, word_next = _context_words(text, index)

        if locked[index]:
            continue

        if char == "的":
            tokens[index] = "de"
            continue

        if char == "得":
            if word_prev in DE_MUST_WORDS or word_next in DE_MUST_WORDS:
                tokens[index] = "děi"
            elif word_prev in DE_NEUTRAL_WORDS or word_next in DE_NEUTRAL_WORDS:
                tokens[index] = "de"
            elif word_prev in DE_FULL_TONE_WORDS or word_next in DE_FULL_TONE_WORDS:
                tokens[index] = "dé"
            elif prev_is_hanzi and next_is_hanzi:
                tokens[index] = "de"

        elif char == "地":
            in_noun_word = word_prev in DI_NOUN_WORDS or word_next in DI_NOUN_WORDS
            if prev_is_hanzi and next_is_hanzi and not in_noun_word and next_char != "的":
                tokens[index] = "de"


def apply_polyphone_overrides(
    text: str,
    tokens: list[str],
    *,
    is_hanzi_char: Callable[[str], bool],
) -> list[str]:
    calibrated = list(tokens)
    locked = apply_phrase_readings(text, calibrated)
    apply_contextual_readings(text, calibrated, locked, is_hanzi_char=is_hanzi_char)
    return calibrated


def phrase_guardrail_errors(
    *,
    para_index: int,
    text: str,
    readings: list[str],
    phrase_readings: PhraseReadings = POLYPHONE_GUARDRAIL_PHRASES,
) -> list[str]:
    errors: list[str] = []
    for phrase, expected_readings in sorted(phrase_readings.items(), key=lambda item: len(item[0]), reverse=True):
        for start in _find_phrase_starts(text, phrase):
            for offset, expected in enumerate(expected_readings):
                index = start + offset
                if not expected:
                    continue
                actual = readings[index] if index < len(readings) else ""
                if actual and actual != expected:
                    errors.append(
                        f"paragraph {para_index} cell {index + 1} {text[index]} in {phrase} "
                        f"should read '{expected}', got '{actual}'"
                    )
    return errors
