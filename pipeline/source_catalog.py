STORY_IDS = [
    "peach-garden-oath",
    "three-heroes-vs-lubu",
    "quench-thirst-plums",
    "three-visits-cottage",
    "zhaoyun-changban",
    "borrow-arrows-boats",
    "red-cliffs",
    "huarong-path",
    "seven-captures",
    "empty-fort",
]


PUBLIC_DOMAIN_NOTE = (
    "Public domain source text. 《三国演义》 is attributed to Luo Guanzhong "
    "(Ming dynasty); 《世说新语》 is attributed to Liu Yiqing (Southern Song "
    "of the Liu Song dynasty). The underlying classical works are in the public domain. "
    "The linked Wikisource pages provide freely available transcriptions."
)


SOURCE_RECORDS = {
    "peach-garden-oath": {
        "title": "桃园三结义",
        "source_title": "《三国演义》第001回 宴桃园豪杰三结义 斩黄巾英雄首立功",
        "source_url": "https://zh.wikisource.org/wiki/三國演義/第001回",
        "related_urls": ["https://zh.wikisource.org/wiki/三國演義/第001回"],
        "license": PUBLIC_DOMAIN_NOTE,
        "summary": "刘备、关羽、张飞因共同愿望相识，在桃园结为兄弟，约定同心协力、救困扶危。",
        "excerpts": [
            "飞曰：吾庄后有一桃园，花开正盛。",
            "三人焚香再拜而说誓曰：既结为兄弟，则同心协力，救困扶危。",
        ],
        "rewrite_focus": "保留友情和共同责任，去掉祭礼、兵器和后续战斗细节。",
    },
    "three-heroes-vs-lubu": {
        "title": "三英战吕布",
        "source_title": "《三国演义》第005回 发矫诏诸镇应曹公 破关兵三英战吕布",
        "source_url": "https://zh.wikisource.org/wiki/三國演義/第005回",
        "related_urls": ["https://zh.wikisource.org/wiki/三國演義/第005回"],
        "license": PUBLIC_DOMAIN_NOTE,
        "summary": "虎牢关前，吕布声势很盛，刘备、关羽、张飞三人互相配合，以团队力量稳住局面。",
        "excerpts": [
            "第005回回目写有：破关兵三英战吕布。",
            "原文以刘备、关羽、张飞三人合力应对吕布为桥段核心。",
        ],
        "rewrite_focus": "将正面打斗改写为队形、配合和勇气，不描写伤害。",
    },
    "quench-thirst-plums": {
        "title": "望梅止渴",
        "source_title": "《世说新语·假谲》魏武行役失汲道",
        "source_url": "https://zh.wikisource.org/zh-hans/世說新語/假譎",
        "related_urls": ["https://zh.wikisource.org/zh-hans/世說新語/假譎"],
        "license": PUBLIC_DOMAIN_NOTE,
        "summary": "曹操带队行路时缺水，借梅林的想象鼓励大家继续前进，后来找到水源。",
        "excerpts": [
            "魏武行役，失汲道，军皆渴。",
            "前有大梅林，饶子，甘酸，可以解渴。",
        ],
        "rewrite_focus": "说明此篇为曹操相关公有领域成语典故，强调鼓励和坚持，不渲染艰险。",
    },
    "three-visits-cottage": {
        "title": "三顾茅庐",
        "source_title": "《三国演义》第037回 司马徽再荐名士 刘玄德三顾草庐",
        "source_url": "https://zh.wikisource.org/wiki/三國演義/第037回",
        "related_urls": [
            "https://zh.wikisource.org/wiki/三國演義/第037回",
            "https://zh.wikisource.org/wiki/三國演義/第038回",
        ],
        "license": PUBLIC_DOMAIN_NOTE,
        "summary": "刘备听说诸葛亮有才学，多次到草庐拜访，最终以真诚和尊重请他出山相助。",
        "excerpts": [
            "第037回回目写有：刘玄德三顾草庐。",
            "刘备多次前往隆中，表现出求贤的耐心与礼貌。",
        ],
        "rewrite_focus": "突出尊重、耐心和请教精神，弱化政治争夺。",
    },
    "zhaoyun-changban": {
        "title": "赵云长坂坡救主",
        "source_title": "《三国演义》第041回 刘玄德携民渡江 赵子龙单骑救主",
        "source_url": "https://zh.wikisource.org/wiki/三國演義/第041回",
        "related_urls": ["https://zh.wikisource.org/wiki/三國演義/第041回"],
        "license": PUBLIC_DOMAIN_NOTE,
        "summary": "长坂坡上队伍混乱，赵云冷静寻找刘备家人，护送孩子平安回到刘备身边。",
        "excerpts": [
            "第041回回目写有：刘玄德携民渡江，赵子龙单骑救主。",
            "原桥段核心是赵云在混乱中守护幼主。",
        ],
        "rewrite_focus": "只写寻找、守护和平安返回，不写战斗伤害。",
    },
    "borrow-arrows-boats": {
        "title": "草船借箭",
        "source_title": "《三国演义》第046回 用奇谋孔明借箭 献密计黄盖受刑",
        "source_url": "https://zh.wikisource.org/wiki/三國演義/第046回",
        "related_urls": ["https://zh.wikisource.org/wiki/三國演義/第046回"],
        "license": PUBLIC_DOMAIN_NOTE,
        "summary": "诸葛亮利用大雾、草船和鼓声，在约定时间内借来十万枝箭，表现出观察与从容。",
        "excerpts": [
            "第046回回目写有：用奇谋孔明借箭。",
            "诸葛亮约定三日交箭，并准备草船、草把和鼓声。",
        ],
        "rewrite_focus": "强调观察天气、守约和巧思，避免战争伤害描写。",
    },
    "red-cliffs": {
        "title": "火烧赤壁",
        "source_title": "《三国演义》第049回 七星坛诸葛祭风 三江口周瑜纵火",
        "source_url": "https://zh.wikisource.org/wiki/三國演義/第049回",
        "related_urls": [
            "https://zh.wikisource.org/wiki/三國演義/第049回",
            "https://zh.wikisource.org/wiki/三國演義/第050回",
        ],
        "license": PUBLIC_DOMAIN_NOTE,
        "summary": "赤壁江边，孙刘两方合作，观察风向与船队形势，用计让曹操大军后退。",
        "excerpts": [
            "第049回回目写有：三江口周瑜纵火。",
            "赤壁桥段重点在周瑜、诸葛亮等人的配合与时机判断。",
        ],
        "rewrite_focus": "将火攻改写为江面信号与船队后退，不描写人员伤亡。",
    },
    "huarong-path": {
        "title": "华容道义释曹操",
        "source_title": "《三国演义》第050回 诸葛亮智算华容 关云长义释曹操",
        "source_url": "https://zh.wikisource.org/wiki/三國演義/第050回",
        "related_urls": ["https://zh.wikisource.org/wiki/三國演義/第050回"],
        "license": PUBLIC_DOMAIN_NOTE,
        "summary": "华容道上，关羽念及旧日恩情，选择以仁义放行曹操一行，表现知恩和担当。",
        "excerpts": [
            "第050回回目写有：关云长义释曹操。",
            "原桥段以关羽在华容道作出仁义选择为核心。",
        ],
        "rewrite_focus": "突出感恩、仁义和承担选择，不写追击细节。",
    },
    "seven-captures": {
        "title": "七擒孟获",
        "source_title": "《三国演义》第088-090回 诸葛亮七擒孟获相关桥段",
        "source_url": "https://zh.wikisource.org/wiki/三國演義/第088回",
        "related_urls": [
            "https://zh.wikisource.org/wiki/三國演義/第088回",
            "https://zh.wikisource.org/wiki/三國演義/第089回",
            "https://zh.wikisource.org/wiki/三國演義/第090回",
        ],
        "license": PUBLIC_DOMAIN_NOTE,
        "summary": "诸葛亮多次放回孟获，希望以诚意让对方心服，最终换来理解与和好。",
        "excerpts": [
            "孔明笑曰：吾擒此人，如囊中取物耳。",
            "直须降伏其心，自然平矣。",
        ],
        "rewrite_focus": "避免刻板化称呼，写成南方首领与蜀军之间的沟通、宽容和信服。",
    },
    "empty-fort": {
        "title": "空城计",
        "source_title": "《三国演义》第095回 马谡拒谏失街亭 武侯弹琴退仲达",
        "source_url": "https://zh.wikisource.org/wiki/三國演義/第095回",
        "related_urls": ["https://zh.wikisource.org/wiki/三國演義/第095回"],
        "license": PUBLIC_DOMAIN_NOTE,
        "summary": "诸葛亮在城中人手很少时保持沉着，打开城门、在城楼弹琴，使司马懿谨慎退去。",
        "excerpts": [
            "第095回回目写有：武侯弹琴退仲达。",
            "原桥段核心是诸葛亮临危不乱，以沉着和判断化解危机。",
        ],
        "rewrite_focus": "强调冷静判断和心理策略，不渲染危险。",
    },
}


def source_record(story_id: str) -> dict:
    return SOURCE_RECORDS[story_id]
