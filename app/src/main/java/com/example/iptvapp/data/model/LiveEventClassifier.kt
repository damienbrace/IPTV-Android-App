package com.example.iptvapp.data.model

private val explicitLivePattern = Regex("""\blive\b""", RegexOption.IGNORE_CASE)
private val sportsContextPattern = Regex(
    """\b(sports?|football|soccer|cricket|rugby|tennis|golf|racing|motorsports?|f1|formula\s*1|boxing|mma|ufc|wwe|nfl|nba|nhl|mlb|afl|nrl)\b""",
    RegexOption.IGNORE_CASE
)
private val eventPattern = Regex(
    """(\s(?:v|vs\.?|versus)\s)|\b(match|game|race|grand prix|test|odi|t20|final|semi[- ]?final|quarter[- ]?final|tournament|championship|cup|open|qualifying|practice|round|stage|heat|session|fight|bout|innings)\b""",
    RegexOption.IGNORE_CASE
)
private val nonLivePattern = Regex(
    """\b(replays?|repeats?|highlights?|reviews?|previews?|build[- ]?ups?|classics?|archives?|reruns?|encores?|delayed|best of|documentar(?:y|ies)|magazine|news|talk|analysis|countdown)\b""",
    RegexOption.IGNORE_CASE
)

internal fun isLikelyLiveSportsEvent(
    title: String,
    description: String?,
    channelName: String,
    channelCategory: String
): Boolean {
    val programmeText = "$title ${description.orEmpty()}"
    if (nonLivePattern.containsMatchIn(programmeText)) return false

    val channelText = "$channelName $channelCategory"
    val hasSportsContext = sportsContextPattern.containsMatchIn(channelText) ||
        sportsContextPattern.containsMatchIn(programmeText)
    if (!hasSportsContext) return false

    return explicitLivePattern.containsMatchIn(programmeText) ||
        eventPattern.containsMatchIn(programmeText)
}
